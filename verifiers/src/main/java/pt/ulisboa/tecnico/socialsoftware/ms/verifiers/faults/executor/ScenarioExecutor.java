package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFinalizationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowStepRecoveryResult;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorBoundaryContext;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorFault;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorProviderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.InMemoryFaultVectorProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.export.EnrichedScenarioCatalogWriter;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.WorkloadDynamicEvidenceRecord;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogPackageReader;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationCheckpoint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenario;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioAction;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioActionKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ForwardFaultSlot;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;

public final class ScenarioExecutor {
    private final ScenarioCatalogReader reader;
    private final ScenarioMaterializer materializer;
    private final ObjectMapper mapper;

    public ScenarioExecutor() {
        this(new ScenarioCatalogReader(), new ScenarioMaterializer(), new ObjectMapper());
    }

    ScenarioExecutor(ScenarioCatalogReader reader, ScenarioMaterializer materializer, ObjectMapper mapper) {
        this.reader = Objects.requireNonNull(reader);
        this.materializer = Objects.requireNonNull(materializer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    public ScenarioExecutionReport execute(ScenarioExecutorOptions options, ScenarioRuntimeContext runtimeContext) {
        Objects.requireNonNull(options, "executor options are required");
        Objects.requireNonNull(runtimeContext, "scenario runtime context is required");
        ScenarioCatalogPackageReader.PackageContents packageContents = reader.read(options);
        rejectPackageOutputAlias(options, packageContents);
        String attemptId = UUID.randomUUID().toString();
        FaultScenario scenario = packageContents.faultScenarios().stream()
                .filter(candidate -> Objects.equals(candidate.deterministicId(), options.faultScenarioId()))
                .findFirst()
                .orElse(null);
        ScenarioExecutionReport report;
        if (scenario == null) {
            ScenarioExecutionReport.Blocker blocker = new ScenarioExecutionReport.Blocker(
                    null, options.faultScenarioId(), null, null, null, null,
                    "MISSING_FAULT_SCENARIO_ID", options.faultScenarioId());
            report = report(options, attemptId, "SELECTION_FAILED", null, null, "NONE", null,
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(blocker));
        } else {
            WorkloadPlan workload = packageContents.workloadPlans().stream()
                    .filter(candidate -> Objects.equals(candidate.deterministicId(), scenario.workloadPlanId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Selected FaultScenario references a missing WorkloadPlan"));
            report = executeSelected(options, runtimeContext, attemptId, workload, scenario);
        }
        writeReport(options, report);
        return report;
    }

    private ScenarioExecutionReport executeSelected(ScenarioExecutorOptions options,
                                                    ScenarioRuntimeContext runtimeContext,
                                                    String attemptId,
                                                    WorkloadPlan workload,
                                                    FaultScenario scenario) {
        ResolvedContract contract = resolveContract(workload, scenario);
        List<ParticipantState> participants = participantStates(workload);
        if (options.dryRun()) {
            return report(options, attemptId, "DRY_RUN", workload, scenario, "NONE", null,
                    contract.faultSlots(), contract.plannedActions(), List.of(), List.of(), participants, List.of());
        }

        List<ScenarioExecutionReport.Blocker> blockers = materializeAll(
                workload, scenario, participants, runtimeContext);
        if (!blockers.isEmpty()) {
            return report(options, attemptId, "MATERIALIZATION_FAILED", workload, scenario, "NONE", null,
                    contract.faultSlots(), contract.plannedActions(), List.of(), List.of(), participants, blockers);
        }
        blockers = startAll(workload, scenario, participants);
        if (!blockers.isEmpty()) {
            return report(options, attemptId, "STARTUP_FAILED", workload, scenario, "NONE", null,
                    contract.faultSlots(), contract.plannedActions(), List.of(), List.of(), participants, blockers);
        }
        return replay(options, attemptId, workload, scenario, contract, participants);
    }

    private List<ScenarioExecutionReport.Blocker> materializeAll(WorkloadPlan workload,
                                                                 FaultScenario scenario,
                                                                 List<ParticipantState> participants,
                                                                 ScenarioRuntimeContext runtimeContext) {
        List<ScenarioExecutionReport.Blocker> blockers = new ArrayList<>();
        for (ParticipantState participant : participants) {
            try {
                participant.unitOfWork = (UnitOfWork) runtimeContext.createSagaUnitOfWork(participant.saga.deterministicId());
                ScenarioMaterializer.MaterializedArguments result = materializer.materialize(
                        participant.input, runtimeContext, participant.saga.sagaFqn(), participant.unitOfWork);
                if (result.success()) {
                    participant.materializedArguments = result.values();
                    participant.materializationState = "MATERIALIZED";
                } else {
                    participant.materializationState = "MATERIALIZATION_FAILED";
                    List<ScenarioExecutionReport.Blocker> owned = result.blockers().stream()
                            .map(blocker -> new ScenarioExecutionReport.Blocker(
                                    workload.deterministicId(), scenario.deterministicId(), blocker.inputVariantId(),
                                    blocker.argumentIndex(), null, blocker.sourceScheduledStepId(),
                                    blocker.reason(), blocker.message()))
                            .toList();
                    participant.blockers.addAll(owned);
                    blockers.addAll(owned);
                }
            } catch (RuntimeException failure) {
                participant.materializationState = "MATERIALIZATION_FAILED";
                ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, null, null,
                        "MATERIALIZATION_FAILED", failure);
                participant.blockers.add(blocker);
                blockers.add(blocker);
            }
        }
        return blockers;
    }

    private List<ScenarioExecutionReport.Blocker> startAll(WorkloadPlan workload,
                                                           FaultScenario scenario,
                                                           List<ParticipantState> participants) {
        List<ScenarioExecutionReport.Blocker> blockers = new ArrayList<>();
        for (ParticipantState participant : participants) {
            try {
                Object instance = instantiate(Class.forName(participant.saga.sagaFqn()), participant.materializedArguments);
                if (!(instance instanceof WorkflowFunctionality functionality)) {
                    throw new IllegalArgumentException(participant.saga.sagaFqn()
                            + " is not a WorkflowFunctionality and cannot use persisted Saga controls");
                }
                participant.functionality = functionality;
                participant.startupState = "STARTUP_READY";
            } catch (ReflectiveOperationException | RuntimeException failure) {
                participant.startupState = "STARTUP_FAILED";
                ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, null, null,
                        "STARTUP_FAILED", unwrap(failure));
                participant.blockers.add(blocker);
                blockers.add(blocker);
            }
        }
        return blockers;
    }

    private ScenarioExecutionReport replay(ScenarioExecutorOptions options,
                                           String attemptId,
                                           WorkloadPlan workload,
                                           FaultScenario scenario,
                                           ResolvedContract contract,
                                           List<ParticipantState> participants) {
        Map<String, ParticipantState> participantsById = new LinkedHashMap<>();
        participants.forEach(participant -> participantsById.put(participant.saga.deterministicId(), participant));
        Map<String, MutableFaultSlot> faultSlots = mutableFaultSlots(contract.faultSlots());
        List<ScenarioExecutionReport.ActionOutcome> actualActions = new ArrayList<>();
        List<ScenarioExecutionReport.LifecycleEvent> lifecycleEvents = new ArrayList<>();
        List<ScenarioExecutionReport.Blocker> blockers = new ArrayList<>();
        Map<String, String> finalFaultSlotByParticipant = new HashMap<>();
        workload.faultSlots().forEach(slot -> finalFaultSlotByParticipant.put(slot.sagaInstanceId(), slot.deterministicId()));
        Map<String, Integer> plannedCompensations = new HashMap<>();
        scenario.actions().stream()
                .filter(action -> action.kind() == FaultScenarioActionKind.COMPENSATION)
                .forEach(action -> plannedCompensations.merge(action.sagaInstanceId(), 1, Integer::sum));

        InMemoryFaultVectorProvider provider = provider(attemptId, workload, scenario, participantsById);
        try (FaultVectorProviderHolder.Scope ignored = FaultVectorProviderHolder.install(provider)) {
            for (int plannedPosition = 0; plannedPosition < scenario.actions().size(); plannedPosition++) {
                FaultScenarioAction action = scenario.actions().get(plannedPosition);
                ParticipantState participant = participantsById.get(action.sagaInstanceId());
                ResolvedAction resolved = contract.actionsById().get(action.deterministicId());
                if (participant == null || terminal(participant.finalState)) {
                    ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, action,
                            resolved == null ? null : resolved.sourceScheduledStepId(),
                            "TERMINAL_PARTICIPANT_ACTION", "action targets a terminal or missing participant");
                    blockers.add(blocker);
                    if (participant != null) participant.blockers.add(blocker);
                    return report(options, attemptId, "UNEXPECTED_EXECUTION_FAILURE", workload, scenario,
                            "IN_MEMORY_FAULT_VECTOR", actualActions.isEmpty() ? null : "INCOMPLETE",
                            snapshot(faultSlots), contract.plannedActions(), actualActions, lifecycleEvents, participants, blockers);
                }
                if ("NOT_STARTED".equals(participant.finalState)) {
                    participant.finalState = "ACTIVE";
                }
                if (action.kind() == FaultScenarioActionKind.FORWARD) {
                    ForwardFaultSlot slot = resolved.faultSlot();
                    int assignedBit = scenario.assignedVector().charAt(slot.slotIndex()) - '0';
                    FaultVectorBoundaryContext boundaryContext = boundaryContext(
                            attemptId, workload, participant, slot, assignedBit);
                    try (FaultVectorProviderHolder.BoundaryScope boundary = FaultVectorProviderHolder.enterBoundary(boundaryContext)) {
                        if (assignedBit == 1) {
                            FaultVectorFault fault = FaultVectorProviderHolder.faultForCurrentBoundary().orElse(null);
                            if (!matches(fault, boundaryContext)) {
                                ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, action,
                                        slot.scheduledStepId(), "FAULT_PROVIDER_MISMATCH",
                                        "persisted assigned fault was not returned for its exact boundary");
                                participant.blockers.add(blocker);
                                blockers.add(blocker);
                                actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                        "FAULT_PROVIDER_MISMATCH", "NOT_RUN", "NOT_RUN", "ASSIGNED", List.of(),
                                        null, blocker.message()));
                                return report(options, attemptId, "FAULT_PROVIDER_MISMATCH", workload, scenario,
                                        "IN_MEMORY_FAULT_VECTOR", "INCOMPLETE", snapshot(faultSlots),
                                        contract.plannedActions(), actualActions, lifecycleEvents, participants, blockers);
                            }
                            participant.functionality.abortBeforeStepForExecutor(slot.runtimeStepName(), participant.unitOfWork);
                            markRealizedAndMasked(faultSlots, slot);
                            participant.finalState = "ABORTED";
                            participant.skippedForwardActions.addAll(skippedForFailure(workload, scenario, slot));
                            actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                    "ASSIGNED_FAULT", "NOT_RUN", "NOT_RUN", "ASSIGNED", List.of(), null, null));
                            lifecycleEvents.add(event(lifecycleEvents, participant, "ABORTED", action, "ASSIGNED_FAULT", null));
                            if (plannedCompensations.getOrDefault(participant.saga.deterministicId(), 0) == 0) {
                                participant.finalState = "COMPENSATED";
                                lifecycleEvents.add(event(lifecycleEvents, participant, "NO_COMPENSATION_WORK", action, "SUCCEEDED", null));
                                lifecycleEvents.add(event(lifecycleEvents, participant, "COMPENSATED", action, "SUCCEEDED", null));
                            }
                        } else {
                            try {
                                participant.functionality.executeStepForExecutor(slot.runtimeStepName(), participant.unitOfWork);
                            } catch (Throwable failure) {
                                Throwable cause = unwrap(failure);
                                participant.finalState = "ABORTED";
                                actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                        "FAILED", "FAILED", "NOT_RUN", "UNASSIGNED_RUNTIME", List.of(), cause, null));
                                lifecycleEvents.add(event(lifecycleEvents, participant, "ABORTED", action, "FORWARD_FAILED", cause));
                                ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, action,
                                        slot.scheduledStepId(), "UNASSIGNED_RUNTIME_FORWARD_FAILURE", cause);
                                participant.blockers.add(blocker);
                                blockers.add(blocker);
                                return report(options, attemptId, "UNEXPECTED_EXECUTION_FAILURE", workload, scenario,
                                        "IN_MEMORY_FAULT_VECTOR", "INCOMPLETE", snapshot(faultSlots),
                                        contract.plannedActions(), actualActions, lifecycleEvents, participants, blockers);
                            }
                            if (Objects.equals(finalFaultSlotByParticipant.get(slot.sagaInstanceId()), slot.deterministicId())) {
                                WorkflowFinalizationResult finalization = participant.functionality.finalizeForExecutor(participant.unitOfWork);
                                if (finalization.committed()) {
                                    participant.finalState = "COMMITTED";
                                    actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                            "COMPLETED", "SUCCEEDED", "SUCCEEDED", null, List.of(), null, null));
                                    lifecycleEvents.add(event(lifecycleEvents, participant, "AUTOMATIC_COMMIT", action, "SUCCEEDED", null));
                                } else {
                                    Throwable cause = unwrap(finalization.failure());
                                    participant.finalState = "ABORTED";
                                    actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                            "COMMIT_FAILED", "SUCCEEDED", "FAILED", "UNASSIGNED_RUNTIME", List.of(), cause, null));
                                    lifecycleEvents.add(event(lifecycleEvents, participant, "ABORTED", action, "COMMIT_FAILED", cause));
                                    ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, action,
                                            slot.scheduledStepId(), "UNASSIGNED_RUNTIME_COMMIT_FAILURE", cause);
                                    participant.blockers.add(blocker);
                                    blockers.add(blocker);
                                    return report(options, attemptId, "UNEXPECTED_EXECUTION_FAILURE", workload, scenario,
                                            "IN_MEMORY_FAULT_VECTOR", "INCOMPLETE", snapshot(faultSlots),
                                            contract.plannedActions(), actualActions, lifecycleEvents, participants, blockers);
                                }
                            } else {
                                actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                        "COMPLETED", "SUCCEEDED", "NOT_RUN", null, List.of(), null, null));
                            }
                        }
                    }
                } else {
                    try {
                        WorkflowStepRecoveryResult recovery = participant.functionality.recoverStepForExecutor(
                                resolved.runtimeStepName(), participant.unitOfWork);
                        List<ScenarioExecutionReport.RecoverySubOutcome> subOutcomes = recoverySubOutcomes(recovery);
                        actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                "COMPENSATED", "NOT_APPLICABLE", "NOT_APPLICABLE", null, subOutcomes, null, null));
                        participant.completedCompensations++;
                        if (participant.completedCompensations
                                == plannedCompensations.getOrDefault(participant.saga.deterministicId(), 0)) {
                            participant.finalState = "COMPENSATED";
                            lifecycleEvents.add(event(lifecycleEvents, participant, "COMPENSATED", action, "SUCCEEDED", null));
                        }
                    } catch (Throwable failure) {
                        Throwable cause = unwrap(failure);
                        participant.finalState = "COMPENSATION_FAILED";
                        actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                "COMPENSATION_FAILED", "NOT_APPLICABLE", "NOT_APPLICABLE", null, List.of(), cause, null));
                        ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, action,
                                resolved.sourceScheduledStepId(), "COMPENSATION_FAILED", cause);
                        participant.blockers.add(blocker);
                        blockers.add(blocker);
                        return report(options, attemptId, "COMPENSATION_FAILED", workload, scenario,
                                "IN_MEMORY_FAULT_VECTOR", "INCOMPLETE", snapshot(faultSlots),
                                contract.plannedActions(), actualActions, lifecycleEvents, participants, blockers);
                    }
                }
            }
        } catch (RuntimeException failure) {
            ScenarioExecutionReport.Blocker blocker = new ScenarioExecutionReport.Blocker(
                    workload.deterministicId(), scenario.deterministicId(), null, null, null, null,
                    "EXECUTOR_INFRASTRUCTURE_FAILURE", failureDetails(unwrap(failure)));
            blockers.add(blocker);
            return report(options, attemptId, "UNEXPECTED_EXECUTION_FAILURE", workload, scenario,
                    "IN_MEMORY_FAULT_VECTOR", actualActions.isEmpty() ? null : "INCOMPLETE", snapshot(faultSlots),
                    contract.plannedActions(), actualActions, lifecycleEvents, participants, blockers);
        }

        return report(options, attemptId, aggregateStatus(participants), workload, scenario,
                "IN_MEMORY_FAULT_VECTOR", "EXACT", snapshot(faultSlots), contract.plannedActions(),
                actualActions, lifecycleEvents, participants, blockers);
    }

    private ResolvedContract resolveContract(WorkloadPlan workload, FaultScenario scenario) {
        Map<String, ScheduledStep> stepsById = new HashMap<>();
        workload.forwardSchedule().forEach(step -> stepsById.put(step.deterministicId(), step));
        Map<String, ForwardFaultSlot> slotsById = new HashMap<>();
        workload.faultSlots().forEach(slot -> slotsById.put(slot.deterministicId(), slot));
        Map<String, CompensationCheckpoint> checkpointsById = new HashMap<>();
        workload.compensationCheckpoints().forEach(checkpoint -> checkpointsById.put(checkpoint.deterministicId(), checkpoint));
        List<ScenarioExecutionReport.FaultSlot> faultSlots = workload.faultSlots().stream()
                .map(slot -> new ScenarioExecutionReport.FaultSlot(
                        slot.slotIndex(), slot.deterministicId(), slot.scheduledStepId(), slot.stepId(),
                        stepsById.get(slot.scheduledStepId()).scheduleOrder(), slot.sagaInstanceId(),
                        slot.runtimeStepName(), scenario.assignedVector().charAt(slot.slotIndex()) - '0',
                        scenario.assignedVector().charAt(slot.slotIndex()) == '1' ? "NOT_REACHED" : "NOT_ASSIGNED", null))
                .toList();
        List<ScenarioExecutionReport.PlannedAction> plannedActions = new ArrayList<>();
        Map<String, ResolvedAction> actionsById = new LinkedHashMap<>();
        for (int index = 0; index < scenario.actions().size(); index++) {
            FaultScenarioAction action = scenario.actions().get(index);
            ForwardFaultSlot slot = slotsById.get(action.sourceFaultSlotId());
            CompensationCheckpoint checkpoint = checkpointsById.get(action.sourceCompensationCheckpointId());
            ScheduledStep source = stepsById.get(slot != null ? slot.scheduledStepId() : checkpoint.sourceScheduledStepId());
            ResolvedAction resolved = new ResolvedAction(
                    action, slot, checkpoint, source.deterministicId(), source.stepId(), source.runtimeStepName(),
                    checkpoint == null ? null : checkpoint.evidenceClass().name());
            actionsById.put(action.deterministicId(), resolved);
            plannedActions.add(new ScenarioExecutionReport.PlannedAction(
                    action.deterministicId(), action.kind().name(), action.sagaInstanceId(),
                    action.sourceFaultSlotId(), action.sourceCompensationCheckpointId(), source.deterministicId(),
                    source.stepId(), source.runtimeStepName(), resolved.compensationEvidenceClass(), index));
        }
        return new ResolvedContract(faultSlots, List.copyOf(plannedActions), Map.copyOf(actionsById));
    }

    private List<ParticipantState> participantStates(WorkloadPlan workload) {
        Map<String, InputVariant> inputsById = new HashMap<>();
        workload.acceptedInputs().forEach(input -> inputsById.put(input.deterministicId(), input));
        return workload.participants().stream()
                .map(saga -> new ParticipantState(saga, inputsById.get(saga.inputVariantId())))
                .toList();
    }

    private InMemoryFaultVectorProvider provider(String attemptId,
                                                  WorkloadPlan workload,
                                                  FaultScenario scenario,
                                                  Map<String, ParticipantState> participants) {
        Map<Integer, FaultVectorFault> assignments = new LinkedHashMap<>();
        for (ForwardFaultSlot slot : workload.faultSlots()) {
            int assignedBit = scenario.assignedVector().charAt(slot.slotIndex()) - '0';
            if (assignedBit == 1) {
                ParticipantState participant = participants.get(slot.sagaInstanceId());
                assignments.put(slot.slotIndex(), FaultVectorFault.from(
                        boundaryContext(attemptId, workload, participant, slot, assignedBit)));
            }
        }
        return new InMemoryFaultVectorProvider(assignments);
    }

    private FaultVectorBoundaryContext boundaryContext(String attemptId,
                                                       WorkloadPlan workload,
                                                       ParticipantState participant,
                                                       ForwardFaultSlot slot,
                                                       int assignedBit) {
        return new FaultVectorBoundaryContext(
                attemptId,
                workload.deterministicId(),
                participant.saga.deterministicId(),
                slot.scheduledStepId(),
                slot.slotIndex(),
                participant.functionality.getClass().getName(),
                participant.functionality.getClass().getSimpleName(),
                slot.runtimeStepName(),
                assignedBit);
    }

    private boolean matches(FaultVectorFault fault, FaultVectorBoundaryContext context) {
        return fault != null
                && Objects.equals(fault.scenarioExecutionId(), context.scenarioExecutionId())
                && Objects.equals(fault.scenarioPlanId(), context.scenarioPlanId())
                && Objects.equals(fault.sagaInstanceId(), context.sagaInstanceId())
                && Objects.equals(fault.scheduledStepId(), context.scheduledStepId())
                && fault.slotIndex() == context.slotIndex()
                && Objects.equals(fault.runtimeStepName(), context.runtimeStepName())
                && fault.assignedBit() == 1;
    }

    private Map<String, MutableFaultSlot> mutableFaultSlots(List<ScenarioExecutionReport.FaultSlot> slots) {
        Map<String, MutableFaultSlot> mutable = new LinkedHashMap<>();
        slots.forEach(slot -> mutable.put(slot.faultSlotId(), new MutableFaultSlot(slot)));
        return mutable;
    }

    private void markRealizedAndMasked(Map<String, MutableFaultSlot> slots, ForwardFaultSlot realized) {
        MutableFaultSlot realizedState = slots.get(realized.deterministicId());
        realizedState.state = "REALIZED";
        for (MutableFaultSlot slot : slots.values()) {
            if (slot.slot.assignedBit() == 1
                    && slot.slot.slotIndex() > realized.slotIndex()
                    && Objects.equals(slot.slot.sagaInstanceId(), realized.sagaInstanceId())) {
                slot.state = "MASKED";
                slot.reason = "masked by earlier realized slot " + realized.slotIndex();
            }
        }
    }

    private List<ScenarioExecutionReport.FaultSlot> snapshot(Map<String, MutableFaultSlot> slots) {
        return slots.values().stream().map(MutableFaultSlot::snapshot).toList();
    }

    private List<ScenarioExecutionReport.SkippedForwardAction> skippedForFailure(WorkloadPlan workload,
                                                                                 FaultScenario scenario,
                                                                                 ForwardFaultSlot failedSlot) {
        Set<String> plannedForwardSlotIds = new HashSet<>();
        scenario.actions().stream()
                .filter(action -> action.kind() == FaultScenarioActionKind.FORWARD)
                .map(FaultScenarioAction::sourceFaultSlotId)
                .forEach(plannedForwardSlotIds::add);
        Map<String, ScheduledStep> stepsById = new HashMap<>();
        workload.forwardSchedule().forEach(step -> stepsById.put(step.deterministicId(), step));
        List<ScenarioExecutionReport.SkippedForwardAction> skipped = new ArrayList<>();
        for (ForwardFaultSlot slot : workload.faultSlots()) {
            if (slot.slotIndex() > failedSlot.slotIndex()
                    && Objects.equals(slot.sagaInstanceId(), failedSlot.sagaInstanceId())
                    && !plannedForwardSlotIds.contains(slot.deterministicId())) {
                int bit = scenario.assignedVector().charAt(slot.slotIndex()) - '0';
                skipped.add(new ScenarioExecutionReport.SkippedForwardAction(
                        slot.deterministicId(), slot.scheduledStepId(), slot.stepId(),
                        stepsById.get(slot.scheduledStepId()).scheduleOrder(), slot.runtimeStepName(), bit,
                        bit == 1 ? "MASKED" : "SKIPPED", "participant aborted before this forward action"));
            }
        }
        return List.copyOf(skipped);
    }

    private ScenarioExecutionReport.ActionOutcome outcome(ResolvedAction action,
                                                          int plannedPosition,
                                                          int actualPosition,
                                                          String status,
                                                          String bodyOutcome,
                                                          String commitOutcome,
                                                          String faultOrigin,
                                                          List<ScenarioExecutionReport.RecoverySubOutcome> recoverySubOutcomes,
                                                          Throwable failure,
                                                          String explicitMessage) {
        return new ScenarioExecutionReport.ActionOutcome(
                action.action().deterministicId(), action.action().kind().name(), action.action().sagaInstanceId(),
                action.action().sourceFaultSlotId(), action.action().sourceCompensationCheckpointId(),
                action.sourceScheduledStepId(), action.sourceStepId(), action.runtimeStepName(),
                action.compensationEvidenceClass(), plannedPosition, actualPosition, status, bodyOutcome, commitOutcome,
                faultOrigin, recoverySubOutcomes, failure == null ? null : failure.getClass().getName(),
                explicitMessage != null ? explicitMessage : failure == null ? null : failure.getMessage());
    }

    private List<ScenarioExecutionReport.RecoverySubOutcome> recoverySubOutcomes(WorkflowStepRecoveryResult recovery) {
        List<ScenarioExecutionReport.RecoverySubOutcome> outcomes = new ArrayList<>();
        if (recovery.explicitCompensationExecuted()) {
            outcomes.add(new ScenarioExecutionReport.RecoverySubOutcome("EXPLICIT_COMPENSATION", "SUCCEEDED"));
        }
        if (recovery.implicitRollbackExecuted()) {
            outcomes.add(new ScenarioExecutionReport.RecoverySubOutcome("IMPLICIT_SAGA_ROLLBACK", "SUCCEEDED"));
        }
        return List.copyOf(outcomes);
    }

    private ScenarioExecutionReport.LifecycleEvent event(List<ScenarioExecutionReport.LifecycleEvent> events,
                                                         ParticipantState participant,
                                                         String type,
                                                         FaultScenarioAction action,
                                                         String outcome,
                                                         Throwable failure) {
        return new ScenarioExecutionReport.LifecycleEvent(
                events.size(), participant.saga.deterministicId(), type, action.deterministicId(), outcome,
                failure == null ? null : failure.getClass().getName(),
                failure == null ? null : failure.getMessage());
    }

    private ScenarioExecutionReport report(ScenarioExecutorOptions options,
                                           String attemptId,
                                           String terminalStatus,
                                           WorkloadPlan workload,
                                           FaultScenario scenario,
                                           String providerMode,
                                           String scheduleConformance,
                                           List<ScenarioExecutionReport.FaultSlot> faultSlots,
                                           List<ScenarioExecutionReport.PlannedAction> plannedActions,
                                           List<ScenarioExecutionReport.ActionOutcome> actualActions,
                                           List<ScenarioExecutionReport.LifecycleEvent> lifecycleEvents,
                                           List<ParticipantState> participantStates,
                                           List<ScenarioExecutionReport.Blocker> blockers) {
        String packagePath = manifestPath(options.packagePath()).toString();
        ScenarioExecutionReport.RuntimeMetadata runtimeMetadata = new ScenarioExecutionReport.RuntimeMetadata(
                options.applicationBase(), options.applicationId(), options.springApplicationClass(), options.springProfiles(),
                options.mavenProfile(), packagePath, options.faultScenarioId(),
                "PERSISTED_FAULT_SCENARIO", options.dryRun());
        List<ScenarioExecutionReport.Participant> participants = participantStates.stream()
                .map(participant -> new ScenarioExecutionReport.Participant(
                        participant.saga.deterministicId(), participant.saga.sagaFqn(),
                        participant.input == null ? null : participant.input.deterministicId(),
                        participant.materializationState, participant.startupState, participant.finalState,
                        participant.skippedForwardActions, participant.blockers))
                .toList();
        return new ScenarioExecutionReport(
                ScenarioExecutionReport.SCHEMA_VERSION, attemptId, terminalStatus, packagePath,
                workload == null ? null : workload.deterministicId(),
                scenario == null ? options.faultScenarioId() : scenario.deterministicId(),
                workload == null ? null : workload.kind().name(),
                scenario == null ? null : scenario.assignedVector(), providerMode, scheduleConformance,
                runtimeMetadata, faultSlots, plannedActions, actualActions, lifecycleEvents, participants, blockers);
    }

    private String aggregateStatus(List<ParticipantState> participants) {
        boolean committed = participants.stream().anyMatch(participant -> "COMMITTED".equals(participant.finalState));
        boolean compensated = participants.stream().anyMatch(participant -> "COMPENSATED".equals(participant.finalState));
        if (committed && compensated) return "PARTIAL_COMPENSATED";
        if (compensated) return "COMPENSATED";
        return "SUCCESS";
    }

    private boolean terminal(String state) {
        return "COMMITTED".equals(state) || "COMPENSATED".equals(state) || "COMPENSATION_FAILED".equals(state);
    }

    private Object instantiate(Class<?> type, List<Object> arguments) throws ReflectiveOperationException {
        for (Constructor<?> constructor : type.getConstructors()) {
            if (constructor.getParameterCount() == arguments.size()) {
                try {
                    return constructor.newInstance(arguments.toArray());
                } catch (IllegalArgumentException ignored) {
                    // Try another constructor with the same arity.
                }
            }
        }
        throw new NoSuchMethodException("No compatible constructor with " + arguments.size() + " arguments for " + type.getName());
    }

    private ScenarioExecutionReport.Blocker blocker(WorkloadPlan workload,
                                                     FaultScenario scenario,
                                                     ParticipantState participant,
                                                     FaultScenarioAction action,
                                                     String sourceScheduledStepId,
                                                     String reason,
                                                     Throwable failure) {
        return blocker(workload, scenario, participant, action, sourceScheduledStepId, reason, failureDetails(failure));
    }

    private ScenarioExecutionReport.Blocker blocker(WorkloadPlan workload,
                                                     FaultScenario scenario,
                                                     ParticipantState participant,
                                                     FaultScenarioAction action,
                                                     String sourceScheduledStepId,
                                                     String reason,
                                                     String message) {
        return new ScenarioExecutionReport.Blocker(
                workload == null ? null : workload.deterministicId(),
                scenario == null ? null : scenario.deterministicId(),
                participant == null || participant.input == null ? null : participant.input.deterministicId(),
                null,
                action == null ? null : action.deterministicId(),
                sourceScheduledStepId,
                reason,
                message);
    }

    private Throwable unwrap(Throwable failure) {
        if (failure == null) return null;
        Throwable current = failure;
        while (current.getCause() != null
                && (current instanceof InvocationTargetException || current instanceof CompletionException)) {
            current = current.getCause();
        }
        return current;
    }

    private String failureDetails(Throwable failure) {
        if (failure == null) return null;
        return failure.getClass().getName() + (failure.getMessage() == null ? "" : ": " + failure.getMessage());
    }

    private Path manifestPath(Path configured) {
        return Files.isDirectory(configured) ? configured.resolve("scenario-catalog-manifest.json") : configured;
    }

    private void rejectPackageOutputAlias(ScenarioExecutorOptions options,
                                          ScenarioCatalogPackageReader.PackageContents packageContents) {
        if (options.outputPath() == null) return;
        Path output = options.outputPath().toAbsolutePath().normalize();
        List<Path> packageInputs = List.of(
                manifestPath(options.packagePath()).toAbsolutePath().normalize(),
                packageContents.workloadCatalogPath(),
                packageContents.faultScenarioCatalogPath(),
                packageContents.accountingPath(),
                packageContents.rejectedInputsPath());
        for (Path packageInput : packageInputs) {
            Path normalizedInput = packageInput.toAbsolutePath().normalize();
            if (output.equals(normalizedInput) || sameFile(output, normalizedInput)) {
                throw new IllegalArgumentException("Scenario execution report output path must not alias scenario package input "
                        + normalizedInput);
            }
        }
        if (isDynamicEnrichmentArtifact(output)) {
            throw new IllegalArgumentException("Scenario execution report output path must not overwrite an existing "
                    + "recognized v3 dynamic-enrichment artifact " + output);
        }
    }

    private boolean isDynamicEnrichmentArtifact(Path output) {
        if (!Files.isRegularFile(output)) return false;
        try {
            JsonNode artifact = mapper.readTree(output.toFile());
            if (artifact == null || !artifact.isObject()) return false;
            String schema = artifact.path("schemaVersion").asText(artifact.path("schema").asText(null));
            return WorkloadDynamicEvidenceRecord.SCHEMA_VERSION.equals(schema)
                    || EnrichedScenarioCatalogWriter.MANIFEST_SCHEMA.equals(schema)
                    || EnrichedScenarioCatalogWriter.JOIN_REPORT_SCHEMA.equals(schema);
        } catch (IOException ignored) {
            return false;
        }
    }

    private boolean sameFile(Path output, Path packageInput) {
        if (!Files.exists(output)) return false;
        try {
            return Files.isSameFile(output, packageInput);
        } catch (IOException failure) {
            throw new IllegalArgumentException("Cannot safely validate scenario execution report output path " + output, failure);
        }
    }

    private void writeReport(ScenarioExecutorOptions options, ScenarioExecutionReport report) {
        if (options.outputPath() == null) return;
        try {
            Path output = options.outputPath().toAbsolutePath().normalize();
            Path parent = output.getParent();
            if (parent != null) Files.createDirectories(parent);
            mapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to write scenario execution report", failure);
        }
    }

    private record ResolvedContract(
            List<ScenarioExecutionReport.FaultSlot> faultSlots,
            List<ScenarioExecutionReport.PlannedAction> plannedActions,
            Map<String, ResolvedAction> actionsById) {
    }

    private record ResolvedAction(
            FaultScenarioAction action,
            ForwardFaultSlot faultSlot,
            CompensationCheckpoint checkpoint,
            String sourceScheduledStepId,
            String sourceStepId,
            String runtimeStepName,
            String compensationEvidenceClass) {
    }

    private static final class ParticipantState {
        private final SagaInstance saga;
        private final InputVariant input;
        private String materializationState = "NOT_ATTEMPTED";
        private String startupState = "NOT_ATTEMPTED";
        private String finalState = "NOT_STARTED";
        private UnitOfWork unitOfWork;
        private List<Object> materializedArguments = List.of();
        private WorkflowFunctionality functionality;
        private int completedCompensations;
        private final List<ScenarioExecutionReport.SkippedForwardAction> skippedForwardActions = new ArrayList<>();
        private final List<ScenarioExecutionReport.Blocker> blockers = new ArrayList<>();

        private ParticipantState(SagaInstance saga, InputVariant input) {
            this.saga = saga;
            this.input = input;
        }
    }

    private static final class MutableFaultSlot {
        private final ScenarioExecutionReport.FaultSlot slot;
        private String state;
        private String reason;

        private MutableFaultSlot(ScenarioExecutionReport.FaultSlot slot) {
            this.slot = slot;
            this.state = slot.state();
            this.reason = slot.reason();
        }

        private ScenarioExecutionReport.FaultSlot snapshot() {
            return new ScenarioExecutionReport.FaultSlot(
                    slot.slotIndex(), slot.faultSlotId(), slot.scheduledStepId(), slot.stepId(), slot.scheduleOrder(),
                    slot.sagaInstanceId(), slot.runtimeStepName(), slot.assignedBit(), state, reason);
        }
    }
}
