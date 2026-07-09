package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorBoundaryContext;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorFault;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorInjectedFaultException;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorProviderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.InMemoryFaultVectorProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletionException;

public class ScenarioExecutor {
    private final ScenarioCatalogReader reader;
    private final ScenarioMaterializer materializer;
    private final ObjectMapper mapper;

    public ScenarioExecutor() {
        this(new ScenarioCatalogReader(), new ScenarioMaterializer(), new ObjectMapper());
    }

    ScenarioExecutor(ScenarioCatalogReader reader, ScenarioMaterializer materializer, ObjectMapper mapper) {
        this.reader = reader;
        this.materializer = materializer;
        this.mapper = mapper;
    }

    public ScenarioExecutionReport execute(ScenarioExecutorOptions options, ScenarioRuntimeContext runtimeContext) {
        List<CatalogScenarioRecord> records = reader.read(options);
        String scenarioExecutionId = UUID.randomUUID().toString();
        ScenarioExecutionReport report;
        if (hasFaultVector(options) && (options.scenarioId() == null || options.scenarioId().isBlank())) {
            report = base(records, scenarioExecutionId, "INVALID_FAULT_VECTOR", "EXPLICIT", "explicit fault vector requires explicit scenario id", null, null,
                    options.faultVector(), null, "EXPLICIT_VECTOR", options, List.of(), List.of(new ScenarioExecutionReport.Blocker(null, null, null, null, "MISSING_EXPLICIT_SCENARIO_ID", "--fault-vector requires --scenario-id")));
        } else {
            report = options.scenarioId() == null || options.scenarioId().isBlank()
                    ? autoSelect(records, options, runtimeContext, scenarioExecutionId)
                    : explicit(records, options, runtimeContext, scenarioExecutionId);
        }
        writeReport(options, report);
        return report;
    }

    private ScenarioExecutionReport explicit(List<CatalogScenarioRecord> records, ScenarioExecutorOptions options, ScenarioRuntimeContext runtimeContext, String scenarioExecutionId) {
        CatalogScenarioRecord record = records.stream()
                .filter(candidate -> options.scenarioId().equals(candidate.plan().deterministicId()))
                .findFirst()
                .orElse(null);
        if (record == null) {
            return base(records, scenarioExecutionId, "SELECTION_FAILED", "EXPLICIT", "requested scenario plan id not found", options.scenarioId(), null,
                    attemptedVector(null, options), null, vectorSource(options), options, List.of(), List.of(new ScenarioExecutionReport.Blocker(options.scenarioId(), null, null, null, "MISSING_SCENARIO_PLAN_ID", options.scenarioId())));
        }
        Candidate candidate = validate(record, options, true);
        if (!candidate.supported()) {
            boolean invalidVector = candidate.blockers().stream().anyMatch(this::isVectorBlocker);
            String status = invalidVector ? "INVALID_FAULT_VECTOR" : "UNSUPPORTED_SCENARIO";
            String reason = invalidVector ? "explicit scenario has invalid fault vector" : "explicit scenario is unsupported";
            return candidateReport(records, scenarioExecutionId, status, "NOT_STARTED", "EXPLICIT", reason, options.scenarioId(), record,
                    attemptedVector(record, options), vectorSource(options), "NONE", options, candidate, List.of(), Map.of(), candidate.blockers());
        }
        return runCandidate(records, record, candidate, options, runtimeContext, scenarioExecutionId, "EXPLICIT", "requested scenario plan id");
    }

    private ScenarioExecutionReport autoSelect(List<CatalogScenarioRecord> records, ScenarioExecutorOptions options, ScenarioRuntimeContext runtimeContext, String scenarioExecutionId) {
        Map<String, Integer> skipped = new TreeMap<>();
        for (CatalogScenarioRecord record : ordered(records)) {
            Candidate candidate = validate(record, options, false);
            if (!candidate.supported()) {
                candidate.blockers().forEach(blocker -> skipped.merge(blocker.reason(), 1, Integer::sum));
                continue;
            }
            if (!options.dryRun()) {
                SagaInstance saga = record.plan().sagaInstances().get(0);
                ScenarioMaterializer.MaterializedArguments args = materializer.materialize(candidate.input(), runtimeContext, saga.sagaFqn());
                if (!args.success()) {
                    args.blockers().forEach(blocker -> skipped.merge(blocker.reason(), 1, Integer::sum));
                    continue;
                }
            }
            ScenarioExecutionReport report = runCandidate(records, record, candidate, options, runtimeContext, scenarioExecutionId, "AUTO", "first materializable single-saga candidate");
            Map<String, Integer> counts = new TreeMap<>(report.skippedCandidateCounts());
            counts.putAll(skipped);
            return new ScenarioExecutionReport(report.schemaVersion(), report.scenarioExecutionId(), report.terminalStatus(), report.catalogPath(), report.catalogKind(), report.selectionMode(), report.selectionReason(), report.requestedScenarioPlanId(), report.scenarioPlanId(), report.scenarioKind(), report.assignedVector(), report.vectorSource(), report.providerMode(), report.runtimeMetadata(), report.faultSlots(), counts, report.blockers(), report.participants());
        }
        return base(records, scenarioExecutionId, "UNSUPPORTED_SCENARIO", "AUTO", "no materializable single-saga candidate", null, null, null, null, "DEFAULT_VECTOR", options, List.of(), List.of());
    }

    private ScenarioExecutionReport runCandidate(List<CatalogScenarioRecord> records,
                                                CatalogScenarioRecord record,
                                                Candidate candidate,
                                                ScenarioExecutorOptions options,
                                                ScenarioRuntimeContext runtimeContext,
                                                String scenarioExecutionId,
                                                String selectionMode,
                                                String selectionReason) {
        ScenarioPlan plan = record.plan();
        SagaInstance saga = plan.sagaInstances().get(0);
        InputVariant input = candidate.input();
        List<ScenarioExecutionReport.StepOutcome> drySteps = candidate.steps().stream()
                .map(step -> new ScenarioExecutionReport.StepOutcome(step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.runtimeName(), "DRY_RUN", null, null))
                .toList();
        if (options.dryRun()) {
            return candidateReport(records, scenarioExecutionId, "DRY_RUN", "NOT_STARTED", selectionMode, selectionReason, options.scenarioId(), record,
                    candidate.vector().value(), candidate.vector().source(), "NONE", options, candidate, drySteps, Map.of(), List.of());
        }
        if (plan.kind() == ScenarioKind.MULTI_SAGA) {
            return runMultiSagaPreparation(records, record, candidate, options, runtimeContext, scenarioExecutionId, selectionMode, selectionReason);
        }
        Object unitOfWork = runtimeContext.createSagaUnitOfWork(saga.sagaFqn());
        ScenarioMaterializer.MaterializedArguments args = materializer.materialize(input, runtimeContext, saga.sagaFqn(), unitOfWork);
        if (!args.success()) {
            return report(records, scenarioExecutionId, "MATERIALIZATION_FAILED", "NOT_STARTED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, candidate.vector().value(), candidate.vector().source(), "NONE", options, candidate.faultSlots(), List.of(), Map.of(), withScenario(plan.deterministicId(), args.blockers()));
        }
        Object functionality;
        try {
            Class<?> sagaClass = Class.forName(saga.sagaFqn());
            functionality = instantiate(sagaClass, args.values());
        } catch (ReflectiveOperationException | RuntimeException e) {
            return report(records, scenarioExecutionId, "STARTUP_FAILED", "NOT_STARTED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, candidate.vector().value(), candidate.vector().source(), "NONE", options, candidate.faultSlots(), List.of(), Map.of(),
                    List.of(new ScenarioExecutionReport.Blocker(plan.deterministicId(), input.deterministicId(), null, null, "STARTUP_FAILED", e.getMessage())));
        }
        List<ScenarioExecutionReport.StepOutcome> outcomes = new ArrayList<>();
        List<ScenarioExecutionReport.FaultSlot> faultSlots = candidate.faultSlots();
        try (FaultVectorProviderHolder.Scope ignored = FaultVectorProviderHolder.install(provider(candidate, scenarioExecutionId, plan, saga, functionality))) {
            for (RuntimeStep step : candidate.steps()) {
                try {
                    try (FaultVectorProviderHolder.BoundaryScope boundary = FaultVectorProviderHolder.enterBoundary(boundaryContext(candidate, step, scenarioExecutionId, plan, saga, functionality))) {
                        Method method = functionality.getClass().getMethod("executeUntilStep", String.class, Class.forName("pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork"));
                        method.invoke(functionality, step.runtimeName(), unitOfWork);
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    if (!(e instanceof InvocationTargetException)) {
                        Throwable failure = unwrap(e);
                        return report(records, scenarioExecutionId, "UNEXPECTED_EXECUTION_FAILURE", "CLOSURE_SKIPPED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, outcomes, Map.of(),
                                List.of(new ScenarioExecutionReport.Blocker(plan.deterministicId(), input.deterministicId(), null, step.scheduled().deterministicId(), "UNEXPECTED_EXECUTION_FAILURE", failureDetails(failure))));
                    }
                    Throwable failure = unwrap(e);
                    if (failure instanceof FaultVectorInjectedFaultException injected) {
                        ScenarioExecutionReport.FaultSlot currentSlot = stepSlot(candidate.faultSlots(), step);
                        outcomes.add(new ScenarioExecutionReport.StepOutcome(step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.runtimeName(), "INJECTED_FAULT", failure.getClass().getName(), failure.getMessage()));
                        if (matchesCurrentSlot(injected, currentSlot)) {
                            faultSlots = realizedAndMaskedSlots(candidate.faultSlots(), injected.getSlotIndex());
                            ClosureResult closure = compensate(functionality, unitOfWork, plan, input, step, failure);
                            if (closure.failed()) {
                                return report(records, scenarioExecutionId, "COMPENSATION_FAILED", "COMPENSATION_FAILED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, outcomes, Map.of(), closure.blockers());
                            }
                            ParticipantRuntimeState participant = new ParticipantRuntimeState(saga, input);
                            participant.materializationState = "MATERIALIZED";
                            participant.startupState = "STARTUP_READY";
                            participant.lifecycleOutcome = closure.lifecycleOutcome();
                            participant.stepOutcomes.addAll(outcomes);
                            participant.skippedSteps.addAll(skippedForwardSteps(candidate.steps(), participant, step.scheduled().scheduleOrder()));
                            return participantStateReport(records, scenarioExecutionId, "COMPENSATED", selectionMode, selectionReason, options.scenarioId(), record,
                                    candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, List.of(participant), List.of());
                        }
                        String status = currentSlot.assignedBit() == 0 ? "UNEXPECTED_INJECTED_FAULT" : "FAULT_PROVIDER_MISMATCH";
                        return report(records, scenarioExecutionId, status, "CLOSURE_SKIPPED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, outcomes, Map.of(),
                                List.of(new ScenarioExecutionReport.Blocker(plan.deterministicId(), input.deterministicId(), null, step.scheduled().deterministicId(), status, failureDetails(failure))));
                    }
                    outcomes.add(new ScenarioExecutionReport.StepOutcome(step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.runtimeName(), "FAILED", failure.getClass().getName(), failure.getMessage()));
                    faultSlots = maskedSlotsAfterFailure(faultSlots, saga.deterministicId(), step.scheduled().scheduleOrder(), "masked by scheduled-step failure after saga abort");
                    ClosureResult closure = compensate(functionality, unitOfWork, plan, input, step, failure);
                    if (closure.failed()) {
                        return report(records, scenarioExecutionId, "COMPENSATION_FAILED", "COMPENSATION_FAILED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, outcomes, Map.of(), closure.blockers());
                    }
                    ParticipantRuntimeState participant = new ParticipantRuntimeState(saga, input);
                    participant.materializationState = "MATERIALIZED";
                    participant.startupState = "STARTUP_READY";
                    participant.lifecycleOutcome = closure.lifecycleOutcome();
                    participant.stepOutcomes.addAll(outcomes);
                    participant.skippedSteps.addAll(skippedForwardSteps(candidate.steps(), participant, step.scheduled().scheduleOrder()));
                    return participantStateReport(records, scenarioExecutionId, "COMPENSATED", selectionMode, selectionReason, options.scenarioId(), record,
                            candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, List.of(participant), List.of());
                }
                ScenarioExecutionReport.FaultSlot slot = stepSlot(candidate.faultSlots(), step);
                if (slot.assignedBit() == 1) {
                    outcomes.add(new ScenarioExecutionReport.StepOutcome(step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.runtimeName(), "EXPECTED_FAULT_NOT_INJECTED", null, null));
                    faultSlots = markExpectedFaultNotInjected(candidate.faultSlots(), slot.slotIndex());
                    return report(records, scenarioExecutionId, "EXPECTED_FAULT_NOT_INJECTED", "CLOSURE_SKIPPED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, outcomes, Map.of(),
                            List.of(new ScenarioExecutionReport.Blocker(plan.deterministicId(), input.deterministicId(), null, step.scheduled().deterministicId(), "EXPECTED_FAULT_NOT_INJECTED", "assigned fault slot " + slot.slotIndex() + " completed without injected fault signal")));
                }
                outcomes.add(new ScenarioExecutionReport.StepOutcome(step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.runtimeName(), "COMPLETED", null, null));
            }
            try {
                Method method = functionality.getClass().getMethod("resumeWorkflow", Class.forName("pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork"));
                method.invoke(functionality, unitOfWork);
            } catch (ReflectiveOperationException | RuntimeException e) {
                Throwable failure = unwrap(e);
                return report(records, scenarioExecutionId, "UNEXPECTED_EXECUTION_FAILURE", "CLOSURE_SKIPPED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, outcomes, Map.of(),
                        List.of(new ScenarioExecutionReport.Blocker(plan.deterministicId(), input.deterministicId(), null, null, "WORKFLOW_CLOSURE_FAILED", failure.getMessage())));
            }
            return report(records, scenarioExecutionId, "SUCCESS", "COMMITTED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, outcomes, Map.of(), List.of());
        }
    }

    private ScenarioExecutionReport runMultiSagaPreparation(List<CatalogScenarioRecord> records,
                                                           CatalogScenarioRecord record,
                                                           Candidate candidate,
                                                           ScenarioExecutorOptions options,
                                                           ScenarioRuntimeContext runtimeContext,
                                                           String scenarioExecutionId,
                                                           String selectionMode,
                                                           String selectionReason) {
        ScenarioPlan plan = record.plan();
        List<ParticipantRuntimeState> participants = candidate.participants().stream()
                .map(participant -> new ParticipantRuntimeState(participant.saga(), participant.input()))
                .toList();
        List<ScenarioExecutionReport.Blocker> blockers = new ArrayList<>();
        for (ParticipantRuntimeState participant : participants) {
            participant.unitOfWork = runtimeContext.createSagaUnitOfWork(participant.saga.sagaFqn());
            ScenarioMaterializer.MaterializedArguments args = materializer.materialize(participant.input, runtimeContext, participant.saga.sagaFqn(), participant.unitOfWork);
            if (args.success()) {
                participant.materializedArguments = args.values();
                participant.materializationState = "MATERIALIZED";
            } else {
                participant.materializationState = "MATERIALIZATION_FAILED";
                participant.blockers.addAll(withScenario(plan.deterministicId(), args.blockers()));
                blockers.addAll(participant.blockers);
            }
        }
        if (!blockers.isEmpty()) {
            return participantStateReport(records, scenarioExecutionId, "MATERIALIZATION_FAILED", selectionMode, selectionReason, options.scenarioId(), record,
                    candidate.vector().value(), candidate.vector().source(), "NONE", options, candidate.faultSlots(), participants, blockers);
        }
        for (ParticipantRuntimeState participant : participants) {
            try {
                Class<?> sagaClass = Class.forName(participant.saga.sagaFqn());
                participant.functionality = instantiate(sagaClass, participant.materializedArguments);
                participant.startupState = "STARTUP_READY";
            } catch (ReflectiveOperationException | RuntimeException e) {
                Throwable failure = unwrap(e);
                participant.startupState = "STARTUP_FAILED";
                participant.blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), participant.input == null ? null : participant.input.deterministicId(), null, null, "STARTUP_FAILED", failureDetails(failure)));
                blockers.addAll(participant.blockers);
            }
        }
        if (!blockers.isEmpty()) {
            return participantStateReport(records, scenarioExecutionId, "STARTUP_FAILED", selectionMode, selectionReason, options.scenarioId(), record,
                    candidate.vector().value(), candidate.vector().source(), "NONE", options, candidate.faultSlots(), participants, blockers);
        }
        return runMultiSagaSchedule(records, record, candidate, options, scenarioExecutionId, selectionMode, selectionReason, participants);
    }

    private ScenarioExecutionReport runMultiSagaSchedule(List<CatalogScenarioRecord> records,
                                                        CatalogScenarioRecord record,
                                                        Candidate candidate,
                                                        ScenarioExecutorOptions options,
                                                        String scenarioExecutionId,
                                                        String selectionMode,
                                                        String selectionReason,
                                                        List<ParticipantRuntimeState> participants) {
        ScenarioPlan plan = record.plan();
        Map<String, ParticipantRuntimeState> bySagaId = new LinkedHashMap<>();
        for (ParticipantRuntimeState participant : participants) {
            bySagaId.put(participant.saga.deterministicId(), participant);
        }
        List<ScenarioExecutionReport.FaultSlot> faultSlots = candidate.faultSlots();
        try (FaultVectorProviderHolder.Scope ignored = FaultVectorProviderHolder.install(provider(candidate, scenarioExecutionId, plan, bySagaId))) {
            for (RuntimeStep step : candidate.steps()) {
                ParticipantRuntimeState participant = bySagaId.get(step.scheduled().sagaInstanceId());
                if (terminal(participant.lifecycleOutcome)) {
                    continue;
                }
                try {
                    try (FaultVectorProviderHolder.BoundaryScope boundary = FaultVectorProviderHolder.enterBoundary(boundaryContext(candidate, step, scenarioExecutionId, plan, participant.saga, participant.functionality))) {
                        Method method = participant.functionality.getClass().getMethod("executeUntilStep", String.class, Class.forName("pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork"));
                        method.invoke(participant.functionality, step.runtimeName(), participant.unitOfWork);
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    if (!(e instanceof InvocationTargetException)) {
                        Throwable failure = unwrap(e);
                        participant.lifecycleOutcome = "CLOSURE_SKIPPED";
                        ScenarioExecutionReport.Blocker blocker = new ScenarioExecutionReport.Blocker(plan.deterministicId(), participant.input == null ? null : participant.input.deterministicId(), null, step.scheduled().deterministicId(), "UNEXPECTED_EXECUTION_FAILURE", failureDetails(failure));
                        participant.blockers.add(blocker);
                        return participantStateReport(records, scenarioExecutionId, "UNEXPECTED_EXECUTION_FAILURE", selectionMode, selectionReason, options.scenarioId(), record,
                                candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, participants, List.of(blocker));
                    }
                    Throwable failure = unwrap(e);
                    if (failure instanceof FaultVectorInjectedFaultException injected) {
                        ScenarioExecutionReport.FaultSlot currentSlot = stepSlot(candidate.faultSlots(), step);
                        participant.stepOutcomes.add(new ScenarioExecutionReport.StepOutcome(step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.runtimeName(), "INJECTED_FAULT", failure.getClass().getName(), failure.getMessage()));
                        if (matchesCurrentSlot(injected, currentSlot, scenarioExecutionId, plan, participant.saga)) {
                            faultSlots = realizedAndMaskedSlots(faultSlots, injected.getSlotIndex(), participant.saga.deterministicId());
                            ClosureResult closure = compensate(participant.functionality, participant.unitOfWork, plan, participant.input, step, failure);
                            participant.lifecycleOutcome = closure.lifecycleOutcome();
                            participant.skippedSteps.addAll(skippedForwardSteps(candidate.steps(), participant, step.scheduled().scheduleOrder()));
                            if (closure.failed()) {
                                participant.blockers.addAll(closure.blockers());
                            }
                            continue;
                        }
                        String status = currentSlot.assignedBit() == 0 ? "UNEXPECTED_INJECTED_FAULT" : "FAULT_PROVIDER_MISMATCH";
                        participant.lifecycleOutcome = "CLOSURE_SKIPPED";
                        ScenarioExecutionReport.Blocker blocker = new ScenarioExecutionReport.Blocker(plan.deterministicId(), participant.input == null ? null : participant.input.deterministicId(), null, step.scheduled().deterministicId(), status, failureDetails(failure));
                        participant.blockers.add(blocker);
                        return participantStateReport(records, scenarioExecutionId, status, selectionMode, selectionReason, options.scenarioId(), record,
                                candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, participants, List.of(blocker));
                    }
                    participant.stepOutcomes.add(new ScenarioExecutionReport.StepOutcome(step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.runtimeName(), "FAILED", failure.getClass().getName(), failure.getMessage()));
                    participant.lastExecutedScheduleOrder = step.scheduled().scheduleOrder();
                    faultSlots = maskedSlotsAfterFailure(faultSlots, participant.saga.deterministicId(), step.scheduled().scheduleOrder(), "masked by scheduled-step failure after saga abort");
                    ClosureResult closure = compensate(participant.functionality, participant.unitOfWork, plan, participant.input, step, failure);
                    participant.lifecycleOutcome = closure.lifecycleOutcome();
                    participant.skippedSteps.addAll(skippedForwardSteps(candidate.steps(), participant, step.scheduled().scheduleOrder()));
                    if (closure.failed()) {
                        participant.blockers.addAll(closure.blockers());
                    }
                    continue;
                }
                ScenarioExecutionReport.FaultSlot slot = stepSlot(candidate.faultSlots(), step);
                if (slot.assignedBit() == 1) {
                    participant.stepOutcomes.add(new ScenarioExecutionReport.StepOutcome(step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.runtimeName(), "EXPECTED_FAULT_NOT_INJECTED", null, null));
                    participant.lifecycleOutcome = "CLOSURE_SKIPPED";
                    faultSlots = markExpectedFaultNotInjected(faultSlots, slot.slotIndex());
                    ScenarioExecutionReport.Blocker blocker = new ScenarioExecutionReport.Blocker(plan.deterministicId(), participant.input == null ? null : participant.input.deterministicId(), null, step.scheduled().deterministicId(), "EXPECTED_FAULT_NOT_INJECTED", "assigned fault slot " + slot.slotIndex() + " completed without injected fault signal");
                    participant.blockers.add(blocker);
                    return participantStateReport(records, scenarioExecutionId, "EXPECTED_FAULT_NOT_INJECTED", selectionMode, selectionReason, options.scenarioId(), record,
                            candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, participants, List.of(blocker));
                }
                participant.lastExecutedScheduleOrder = step.scheduled().scheduleOrder();
                participant.stepOutcomes.add(new ScenarioExecutionReport.StepOutcome(step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.runtimeName(), "COMPLETED", null, null));
            }
            List<ParticipantRuntimeState> closureOrder = participants.stream()
                    .filter(participant -> !terminal(participant.lifecycleOutcome))
                    .sorted(Comparator.comparing((ParticipantRuntimeState participant) -> participant.lastExecutedScheduleOrder == null)
                            .thenComparing(participant -> participant.lastExecutedScheduleOrder == null ? 0 : participant.lastExecutedScheduleOrder)
                            .thenComparing(participant -> participant.saga.deterministicId()))
                    .toList();
            for (ParticipantRuntimeState participant : closureOrder) {
                try {
                    Method method = participant.functionality.getClass().getMethod("resumeWorkflow", Class.forName("pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork"));
                    method.invoke(participant.functionality, participant.unitOfWork);
                    participant.lifecycleOutcome = "COMMITTED";
                } catch (ReflectiveOperationException | RuntimeException e) {
                    Throwable failure = unwrap(e);
                    participant.lifecycleOutcome = "CLOSURE_SKIPPED";
                    ScenarioExecutionReport.Blocker blocker = new ScenarioExecutionReport.Blocker(plan.deterministicId(), participant.input == null ? null : participant.input.deterministicId(), null, null, "WORKFLOW_CLOSURE_FAILED", failureDetails(failure));
                    participant.blockers.add(blocker);
                    return participantStateReport(records, scenarioExecutionId, "UNEXPECTED_EXECUTION_FAILURE", selectionMode, selectionReason, options.scenarioId(), record,
                            candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, participants, List.of(blocker));
                }
            }
            return participantStateReport(records, scenarioExecutionId, aggregateStatus(participants), selectionMode, selectionReason, options.scenarioId(), record,
                    candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, participants, List.of());
        }
    }

    private boolean terminal(String lifecycleOutcome) {
        return switch (lifecycleOutcome) {
            case "COMMITTED", "COMPENSATED", "COMPENSATION_FAILED", "CLOSURE_SKIPPED" -> true;
            default -> false;
        };
    }

    private String aggregateStatus(List<ParticipantRuntimeState> participants) {
        boolean anyCompensationFailed = participants.stream().anyMatch(participant -> "COMPENSATION_FAILED".equals(participant.lifecycleOutcome));
        if (anyCompensationFailed) return "COMPENSATION_FAILED";
        boolean anyCompensated = participants.stream().anyMatch(participant -> "COMPENSATED".equals(participant.lifecycleOutcome));
        boolean anyCommitted = participants.stream().anyMatch(participant -> "COMMITTED".equals(participant.lifecycleOutcome));
        if (anyCompensated && anyCommitted) return "PARTIAL_COMPENSATED";
        if (anyCompensated) return "COMPENSATED";
        return "SUCCESS";
    }

    private List<ScenarioExecutionReport.SkippedStep> skippedForwardSteps(List<RuntimeStep> steps, ParticipantRuntimeState participant, int failedScheduleOrder) {
        return steps.stream()
                .filter(step -> Objects.equals(step.scheduled().sagaInstanceId(), participant.saga.deterministicId()))
                .filter(step -> step.scheduled().scheduleOrder() > failedScheduleOrder)
                .map(step -> new ScenarioExecutionReport.SkippedStep(step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.runtimeName(), "SKIPPED_BY_SAGA_FAILURE"))
                .toList();
    }

    private ScenarioExecutionReport participantStateReport(List<CatalogScenarioRecord> records, String scenarioExecutionId, String status, String mode, String reason, String requested, CatalogScenarioRecord record, String assignedVector, String vectorSource, String providerMode, ScenarioExecutorOptions options, List<ScenarioExecutionReport.FaultSlot> slots, List<ParticipantRuntimeState> participantStates, List<ScenarioExecutionReport.Blocker> blockers) {
        CatalogScenarioRecord source = record != null ? record : records.stream().findFirst().orElse(null);
        String scenarioPlanId = record == null ? null : record.plan().deterministicId();
        ScenarioExecutionReport.RuntimeMetadata runtimeMetadata = new ScenarioExecutionReport.RuntimeMetadata(
                options.applicationBase(),
                options.applicationId(),
                options.springApplicationClass(),
                options.springProfiles(),
                options.mavenProfile(),
                source == null ? null : source.catalogPath(),
                source == null ? null : source.catalogKind(),
                scenarioPlanId,
                vectorSource,
                mode,
                options.dryRun());
        List<ScenarioExecutionReport.Participant> participants = participantStates.stream()
                .map(participant -> new ScenarioExecutionReport.Participant(
                        participant.saga.deterministicId(),
                        participant.saga.sagaFqn(),
                        participant.input == null ? null : participant.input.deterministicId(),
                        participant.materializationState,
                        participant.startupState,
                        participant.lifecycleOutcome,
                        participant.stepOutcomes,
                        participant.skippedSteps,
                        participant.blockers))
                .toList();
        return new ScenarioExecutionReport(ScenarioExecutionReport.SCHEMA_VERSION, scenarioExecutionId, status,
                source == null ? null : source.catalogPath(), source == null ? null : source.catalogKind(), mode, reason, requested,
                scenarioPlanId, record == null ? null : record.plan().kind().name(), assignedVector, vectorSource, providerMode, runtimeMetadata, markNotReached(slots), Map.of(), blockers, participants);
    }

    private List<ScenarioExecutionReport.FaultSlot> markNotReached(List<ScenarioExecutionReport.FaultSlot> slots) {
        return slots.stream()
                .map(slot -> slot.assignedBit() == 1 && "NOT_REACHED".equals(slot.realizationState())
                        ? new ScenarioExecutionReport.FaultSlot(slot.slotIndex(), slot.scheduledStepId(), slot.catalogStepId(), slot.scheduleOrder(), slot.sagaInstanceId(), slot.runtimeStepName(), slot.assignedBit(), "NOT_REACHED", "execution hard-stopped before scheduled runtime execution")
                        : slot)
                .toList();
    }

    private boolean matchesCurrentSlot(FaultVectorInjectedFaultException injected, ScenarioExecutionReport.FaultSlot slot) {
        return injected.getScheduledStepId().equals(slot.scheduledStepId())
                && injected.getRuntimeStepName().equals(slot.runtimeStepName())
                && injected.getSlotIndex() == slot.slotIndex()
                && injected.getAssignedBit() == slot.assignedBit()
                && slot.assignedBit() == 1;
    }

    private boolean matchesCurrentSlot(FaultVectorInjectedFaultException injected, ScenarioExecutionReport.FaultSlot slot, String scenarioExecutionId, ScenarioPlan plan, SagaInstance saga) {
        return matchesCurrentSlot(injected, slot)
                && injected.getScenarioExecutionId().equals(scenarioExecutionId)
                && injected.getScenarioPlanId().equals(plan.deterministicId())
                && injected.getSagaInstanceId().equals(saga.deterministicId());
    }

    private ClosureResult compensate(Object functionality, Object unitOfWork, ScenarioPlan plan, InputVariant input, RuntimeStep step, Throwable forwardFailure) {
        try {
            Method method = functionality.getClass().getMethod("resumeCompensation", Class.forName("pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork"));
            method.invoke(functionality, unitOfWork);
            return new ClosureResult(false, "COMPENSATED", List.of());
        } catch (NoSuchMethodException e) {
            return new ClosureResult(false, "CLOSURE_SKIPPED", List.of());
        } catch (ReflectiveOperationException | RuntimeException compensationError) {
            Throwable compensationFailure = unwrap(compensationError);
            return new ClosureResult(true, "COMPENSATION_FAILED", List.of(
                    new ScenarioExecutionReport.Blocker(plan.deterministicId(), input.deterministicId(), null, step.scheduled().deterministicId(), "FORWARD_FAILURE", failureDetails(forwardFailure)),
                    new ScenarioExecutionReport.Blocker(plan.deterministicId(), input.deterministicId(), null, step.scheduled().deterministicId(), "COMPENSATION_FAILED", failureDetails(compensationFailure))));
        }
    }

    private ScenarioExecutionReport.FaultSlot stepSlot(List<ScenarioExecutionReport.FaultSlot> slots, RuntimeStep step) {
        return slots.stream()
                .filter(slot -> slot.scheduledStepId().equals(step.scheduled().deterministicId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing fault slot for scheduled step " + step.scheduled().deterministicId()));
    }

    private List<ScenarioExecutionReport.FaultSlot> markExpectedFaultNotInjected(List<ScenarioExecutionReport.FaultSlot> slots, int unrealizedSlotIndex) {
        List<ScenarioExecutionReport.FaultSlot> updated = new ArrayList<>();
        for (ScenarioExecutionReport.FaultSlot slot : slots) {
            if (slot.slotIndex() == unrealizedSlotIndex) {
                updated.add(new ScenarioExecutionReport.FaultSlot(slot.slotIndex(), slot.scheduledStepId(), slot.catalogStepId(), slot.scheduleOrder(), slot.sagaInstanceId(), slot.runtimeStepName(), slot.assignedBit(), "EXPECTED_FAULT_NOT_INJECTED", null));
            } else if (slot.slotIndex() > unrealizedSlotIndex && slot.assignedBit() == 1 && "NOT_REACHED".equals(slot.realizationState())) {
                updated.add(new ScenarioExecutionReport.FaultSlot(slot.slotIndex(), slot.scheduledStepId(), slot.catalogStepId(), slot.scheduleOrder(), slot.sagaInstanceId(), slot.runtimeStepName(), slot.assignedBit(), "NOT_REACHED", "execution hard-stopped before scheduled runtime execution"));
            } else {
                updated.add(slot);
            }
        }
        return updated;
    }

    private List<ScenarioExecutionReport.FaultSlot> maskedSlotsAfterFailure(List<ScenarioExecutionReport.FaultSlot> slots, String failedSagaInstanceId, int failedScheduleOrder, String reason) {
        List<ScenarioExecutionReport.FaultSlot> updated = new ArrayList<>();
        for (ScenarioExecutionReport.FaultSlot slot : slots) {
            if (slot.assignedBit() == 1
                    && Objects.equals(slot.sagaInstanceId(), failedSagaInstanceId)
                    && slot.scheduleOrder() > failedScheduleOrder
                    && "NOT_REACHED".equals(slot.realizationState())) {
                updated.add(new ScenarioExecutionReport.FaultSlot(slot.slotIndex(), slot.scheduledStepId(), slot.catalogStepId(), slot.scheduleOrder(), slot.sagaInstanceId(), slot.runtimeStepName(), slot.assignedBit(), "MASKED_BY_SAGA_FAILURE", reason));
            } else {
                updated.add(slot);
            }
        }
        return updated;
    }

    private String failureDetails(Throwable failure) {
        return failure.getClass().getName() + (failure.getMessage() == null ? "" : ": " + failure.getMessage());
    }

    private List<ScenarioExecutionReport.FaultSlot> realizedAndMaskedSlots(List<ScenarioExecutionReport.FaultSlot> slots, int realizedSlotIndex) {
        String failedSagaInstanceId = slots.stream()
                .filter(slot -> slot.slotIndex() == realizedSlotIndex)
                .map(ScenarioExecutionReport.FaultSlot::sagaInstanceId)
                .findFirst()
                .orElse(null);
        return realizedAndMaskedSlots(slots, realizedSlotIndex, failedSagaInstanceId);
    }

    private List<ScenarioExecutionReport.FaultSlot> realizedAndMaskedSlots(List<ScenarioExecutionReport.FaultSlot> slots, int realizedSlotIndex, String failedSagaInstanceId) {
        List<ScenarioExecutionReport.FaultSlot> updated = new ArrayList<>();
        for (ScenarioExecutionReport.FaultSlot slot : slots) {
            if (slot.slotIndex() == realizedSlotIndex) {
                updated.add(new ScenarioExecutionReport.FaultSlot(slot.slotIndex(), slot.scheduledStepId(), slot.catalogStepId(), slot.scheduleOrder(), slot.sagaInstanceId(), slot.runtimeStepName(), slot.assignedBit(), "REALIZED", null));
            } else if (slot.slotIndex() > realizedSlotIndex && slot.assignedBit() == 1 && Objects.equals(slot.sagaInstanceId(), failedSagaInstanceId)) {
                updated.add(new ScenarioExecutionReport.FaultSlot(slot.slotIndex(), slot.scheduledStepId(), slot.catalogStepId(), slot.scheduleOrder(), slot.sagaInstanceId(), slot.runtimeStepName(), slot.assignedBit(), "MASKED_BY_SAGA_FAILURE", "masked by earlier realized slot " + realizedSlotIndex + " after saga abort"));
            } else {
                updated.add(slot);
            }
        }
        return updated;
    }

    private InMemoryFaultVectorProvider provider(Candidate candidate, String scenarioExecutionId, ScenarioPlan plan, SagaInstance saga, Object functionality) {
        Map<Integer, FaultVectorFault> assignments = new LinkedHashMap<>();
        for (ScenarioExecutionReport.FaultSlot slot : candidate.faultSlots()) {
            if (slot.assignedBit() == 1) {
                assignments.put(slot.slotIndex(), FaultVectorFault.from(boundaryContext(slot, scenarioExecutionId, plan, saga, functionality)));
            }
        }
        return new InMemoryFaultVectorProvider(assignments);
    }

    private InMemoryFaultVectorProvider provider(Candidate candidate, String scenarioExecutionId, ScenarioPlan plan, Map<String, ParticipantRuntimeState> participants) {
        Map<Integer, FaultVectorFault> assignments = new LinkedHashMap<>();
        for (ScenarioExecutionReport.FaultSlot slot : candidate.faultSlots()) {
            if (slot.assignedBit() == 1) {
                ParticipantRuntimeState participant = participants.get(slot.sagaInstanceId());
                assignments.put(slot.slotIndex(), FaultVectorFault.from(boundaryContext(slot, scenarioExecutionId, plan, participant.saga, participant.functionality)));
            }
        }
        return new InMemoryFaultVectorProvider(assignments);
    }

    private FaultVectorBoundaryContext boundaryContext(Candidate candidate, RuntimeStep step, String scenarioExecutionId, ScenarioPlan plan, SagaInstance saga, Object functionality) {
        ScenarioExecutionReport.FaultSlot slot = candidate.faultSlots().stream()
                .filter(candidateSlot -> candidateSlot.scheduledStepId().equals(step.scheduled().deterministicId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing fault slot for scheduled step " + step.scheduled().deterministicId()));
        return boundaryContext(slot, scenarioExecutionId, plan, saga, functionality);
    }

    private FaultVectorBoundaryContext boundaryContext(ScenarioExecutionReport.FaultSlot slot, String scenarioExecutionId, ScenarioPlan plan, SagaInstance saga, Object functionality) {
        return new FaultVectorBoundaryContext(
                scenarioExecutionId,
                plan.deterministicId(),
                saga.deterministicId(),
                slot.scheduledStepId(),
                slot.slotIndex(),
                functionality.getClass().getName(),
                functionality.getClass().getSimpleName(),
                slot.runtimeStepName(),
                slot.assignedBit());
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null
                && (current instanceof InvocationTargetException || current instanceof CompletionException)) {
            current = current.getCause();
        }
        return current;
    }

    private Candidate validate(CatalogScenarioRecord record, ScenarioExecutorOptions options, boolean allowExplicitMultiSaga) {
        ScenarioPlan plan = record.plan();
        List<ScenarioExecutionReport.Blocker> blockers = new ArrayList<>();
        boolean multiSaga = plan.kind() == ScenarioKind.MULTI_SAGA;
        if (plan.sagaInstances().isEmpty()) {
            blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), null, null, null, "UNSUPPORTED_SCENARIO_SHAPE", "scenario plan must have at least one saga instance"));
        } else if (plan.kind() == ScenarioKind.SINGLE_SAGA && plan.sagaInstances().size() != 1) {
            blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), null, null, null, "UNSUPPORTED_SCENARIO_SHAPE", "single-saga plans must have exactly one saga instance"));
        } else if (multiSaga && !allowExplicitMultiSaga) {
            blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), null, null, null, "UNSUPPORTED_SCENARIO_SHAPE", "multi-saga plans require explicit scenario id selection"));
        } else if (plan.kind() != ScenarioKind.SINGLE_SAGA && plan.kind() != ScenarioKind.MULTI_SAGA) {
            blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), null, null, null, "UNSUPPORTED_SCENARIO_SHAPE", "unsupported scenario kind " + plan.kind()));
        }

        List<ParticipantCandidate> participants = new ArrayList<>();
        Map<String, ParticipantCandidate> participantsBySagaId = new LinkedHashMap<>();
        for (SagaInstance saga : plan.sagaInstances()) {
            List<InputVariant> matches = plan.inputs().stream()
                    .filter(candidate -> Objects.equals(saga.inputVariantId(), candidate.deterministicId()))
                    .toList();
            InputVariant input = matches.size() == 1 ? matches.get(0) : null;
            if (matches.isEmpty()) {
                blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), saga.inputVariantId(), null, null, "MISSING_INPUT_VARIANT", "saga instance " + saga.deterministicId() + " has no matching input variant"));
            } else if (matches.size() > 1) {
                blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), saga.inputVariantId(), null, null, "DUPLICATE_INPUT_VARIANT", "saga instance " + saga.deterministicId() + " has multiple matching input variants"));
            }
            ParticipantCandidate participant = new ParticipantCandidate(saga, input);
            participants.add(participant);
            participantsBySagaId.put(saga.deterministicId(), participant);
        }

        List<RuntimeStep> steps = new ArrayList<>();
        Set<String> seenScheduledIds = new HashSet<>();
        for (ScheduledStep step : plan.expandedSchedule().stream().sorted(Comparator.comparingInt(ScheduledStep::scheduleOrder)).toList()) {
            if (!seenScheduledIds.add(step.deterministicId())) {
                blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), null, null, step.deterministicId(), "DUPLICATE_SCHEDULED_STEP_ID", step.deterministicId()));
            }
            ParticipantCandidate owner = participantsBySagaId.get(step.sagaInstanceId());
            if (owner == null) {
                blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), null, null, step.deterministicId(), "UNKNOWN_SCHEDULED_STEP_OWNER", step.sagaInstanceId()));
            }
            String runtimeName = runtimeStepName(step.stepId());
            if (runtimeName == null) {
                blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), owner == null || owner.input() == null ? null : owner.input().deterministicId(), null, step.deterministicId(), "UNSUPPORTED_STEP_ID", step.stepId()));
            } else {
                steps.add(new RuntimeStep(step, runtimeName));
            }
        }
        InputVariant input = participants.size() == 1 ? participants.get(0).input() : null;
        AssignedVector vector = validateVector(plan, input, steps, options, blockers);
        return new Candidate(blockers.isEmpty(), input, participants, steps, vector, vector == null ? List.of() : vector.slots(), blockers);
    }

    private AssignedVector validateVector(ScenarioPlan plan, InputVariant input, List<RuntimeStep> steps, ScenarioExecutorOptions options, List<ScenarioExecutionReport.Blocker> blockers) {
        FaultSpace faultSpace = plan.faultSpace();
        String source = vectorSource(options);
        String vector = hasFaultVector(options) ? options.faultVector() : faultSpace.defaultVector();
        String inputId = input == null ? null : input.deterministicId();
        int blockerCount = blockers.size();
        if (faultSpace.length() != faultSpace.scheduledStepIds().size()) {
            blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), inputId, null, null, "FAULT_SPACE_LENGTH_MISMATCH", "faultSpace.length must match scheduledStepIds size"));
        }
        if (vector == null) {
            blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), inputId, null, null, invalidVectorReason(source), "fault vector is missing"));
            return null;
        }
        if (!vector.matches("[01]*")) {
            blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), inputId, null, null, invalidVectorReason(source), "fault vector must be binary"));
        }
        if (vector.length() != faultSpace.length()) {
            blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), inputId, null, null, invalidVectorReason(source), "fault vector length " + vector.length() + " does not match faultSpace.length " + faultSpace.length()));
        }
        if (vector.isEmpty() && faultSpace.length() != 0) {
            blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), inputId, null, null, invalidVectorReason(source), "empty fault vector is valid only for zero-length fault spaces"));
        }
        Set<String> seenIds = new HashSet<>();
        for (String scheduledStepId : faultSpace.scheduledStepIds()) {
            if (!seenIds.add(scheduledStepId)) {
                blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), inputId, null, scheduledStepId, "DUPLICATE_FAULT_SPACE_SCHEDULED_STEP_ID", scheduledStepId));
            }
        }
        Map<String, List<RuntimeStep>> byScheduledId = new LinkedHashMap<>();
        for (RuntimeStep step : steps) {
            byScheduledId.computeIfAbsent(step.scheduled().deterministicId(), ignored -> new ArrayList<>()).add(step);
        }
        List<RuntimeStep> slotSteps = new ArrayList<>();
        Set<String> mappedStepIds = new HashSet<>();
        for (int index = 0; index < faultSpace.scheduledStepIds().size(); index++) {
            String scheduledStepId = faultSpace.scheduledStepIds().get(index);
            List<RuntimeStep> matches = byScheduledId.getOrDefault(scheduledStepId, List.of());
            if (matches.isEmpty()) {
                blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), inputId, null, scheduledStepId, "UNRESOLVED_FAULT_SPACE_SCHEDULED_STEP_ID", scheduledStepId));
            } else if (matches.size() > 1) {
                blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), inputId, null, scheduledStepId, "NON_UNIQUE_FAULT_SLOT_MAPPING", scheduledStepId));
            } else if (!mappedStepIds.add(matches.get(0).scheduled().deterministicId())) {
                blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), inputId, null, scheduledStepId, "NON_UNIQUE_FAULT_SLOT_MAPPING", scheduledStepId));
            } else {
                slotSteps.add(matches.get(0));
            }
        }
        if (blockers.size() > blockerCount) {
            return null;
        }
        List<ScenarioExecutionReport.FaultSlot> slots = new ArrayList<>();
        for (int index = 0; index < slotSteps.size(); index++) {
            RuntimeStep step = slotSteps.get(index);
            int bit = vector.charAt(index) == '1' ? 1 : 0;
            String realizationState = bit == 0 ? "NOT_ASSIGNED" : options.dryRun() ? "DRY_RUN" : "NOT_REACHED";
            slots.add(new ScenarioExecutionReport.FaultSlot(index, step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.scheduled().sagaInstanceId(), step.runtimeName(), bit, realizationState, null));
        }
        return new AssignedVector(vector, source, slots);
    }

    private boolean isVectorBlocker(ScenarioExecutionReport.Blocker blocker) {
        return switch (blocker.reason()) {
            case "INVALID_DEFAULT_VECTOR", "INVALID_EXPLICIT_VECTOR", "FAULT_SPACE_LENGTH_MISMATCH",
                 "DUPLICATE_FAULT_SPACE_SCHEDULED_STEP_ID", "UNRESOLVED_FAULT_SPACE_SCHEDULED_STEP_ID",
                 "NON_UNIQUE_FAULT_SLOT_MAPPING" -> true;
            default -> false;
        };
    }

    private String invalidVectorReason(String source) {
        return "DEFAULT_VECTOR".equals(source) ? "INVALID_DEFAULT_VECTOR" : "INVALID_EXPLICIT_VECTOR";
    }

    private String attemptedVector(CatalogScenarioRecord record, ScenarioExecutorOptions options) {
        if (hasFaultVector(options)) return options.faultVector();
        return record == null ? null : record.plan().faultSpace().defaultVector();
    }

    private boolean hasFaultVector(ScenarioExecutorOptions options) {
        return options.faultVector() != null;
    }

    private String vectorSource(ScenarioExecutorOptions options) {
        return hasFaultVector(options) ? "EXPLICIT_VECTOR" : "DEFAULT_VECTOR";
    }

    static String runtimeStepName(String stepId) {
        if (stepId == null) return null;
        int marker = stepId.lastIndexOf("::");
        if (marker < 0 || marker + 2 >= stepId.length()) return null;
        String name = stepId.substring(marker + 2).replaceFirst("#\\d+$", "").trim();
        return name.isBlank() ? null : name;
    }

    private List<CatalogScenarioRecord> ordered(List<CatalogScenarioRecord> records) {
        return records.stream()
                .sorted(Comparator.comparingInt(this::joinPriority).thenComparingInt(CatalogScenarioRecord::lineNumber))
                .toList();
    }

    private int joinPriority(CatalogScenarioRecord record) {
        if (record.joinStatus() == null) return 99;
        return switch (record.joinStatus()) {
            case MATCHED_EXACT -> 0;
            case MATCHED_HIGH_CONFIDENCE -> 1;
            case MATCHED_PARTIAL -> 2;
            case AMBIGUOUS -> 3;
            case UNMATCHED -> 4;
            case NOT_COVERED -> 5;
        };
    }

    private Object instantiate(Class<?> sagaClass, List<Object> args) throws ReflectiveOperationException {
        for (Constructor<?> constructor : sagaClass.getConstructors()) {
            if (constructor.getParameterCount() == args.size()) return constructor.newInstance(args.toArray());
        }
        throw new NoSuchMethodException("No constructor with " + args.size() + " arguments for " + sagaClass.getName());
    }

    private ScenarioExecutionReport base(List<CatalogScenarioRecord> records, String scenarioExecutionId, String status, String mode, String reason, String requested, CatalogScenarioRecord record, String assignedVector, String lifecycleOutcome, String vectorSource, ScenarioExecutorOptions options, List<ScenarioExecutionReport.FaultSlot> slots, List<ScenarioExecutionReport.Blocker> blockers) {
        SagaInstance saga = record != null && record.plan().sagaInstances().size() == 1 ? record.plan().sagaInstances().get(0) : null;
        InputVariant input = saga == null ? null : record.plan().inputs().stream()
                .filter(candidate -> saga.inputVariantId().equals(candidate.deterministicId()))
                .findFirst()
                .orElse(null);
        return report(records, scenarioExecutionId, status, lifecycleOutcome == null ? "NOT_STARTED" : lifecycleOutcome, mode, reason, requested, record, saga, input, assignedVector, vectorSource, "NONE", options, slots, List.of(), Map.of(), blockers);
    }

    private ScenarioExecutionReport report(List<CatalogScenarioRecord> records, String scenarioExecutionId, String status, String lifecycleOutcome, String mode, String reason, String requested, CatalogScenarioRecord record, SagaInstance saga, InputVariant input, String assignedVector, String vectorSource, String providerMode, ScenarioExecutorOptions options, List<ScenarioExecutionReport.FaultSlot> slots, List<ScenarioExecutionReport.StepOutcome> steps, Map<String, Integer> skipped, List<ScenarioExecutionReport.Blocker> blockers) {
        List<ParticipantCandidate> participants = saga == null ? List.of() : List.of(new ParticipantCandidate(saga, input));
        return report(records, scenarioExecutionId, status, lifecycleOutcome, mode, reason, requested, record, assignedVector, vectorSource, providerMode, options, slots, steps, skipped, blockers, participants);
    }

    private ScenarioExecutionReport candidateReport(List<CatalogScenarioRecord> records, String scenarioExecutionId, String status, String lifecycleOutcome, String mode, String reason, String requested, CatalogScenarioRecord record, String assignedVector, String vectorSource, String providerMode, ScenarioExecutorOptions options, Candidate candidate, List<ScenarioExecutionReport.StepOutcome> steps, Map<String, Integer> skipped, List<ScenarioExecutionReport.Blocker> blockers) {
        return report(records, scenarioExecutionId, status, lifecycleOutcome, mode, reason, requested, record, assignedVector, vectorSource, providerMode, options, candidate.faultSlots(), steps, skipped, blockers, candidate.participants());
    }

    private ScenarioExecutionReport report(List<CatalogScenarioRecord> records, String scenarioExecutionId, String status, String lifecycleOutcome, String mode, String reason, String requested, CatalogScenarioRecord record, String assignedVector, String vectorSource, String providerMode, ScenarioExecutorOptions options, List<ScenarioExecutionReport.FaultSlot> slots, List<ScenarioExecutionReport.StepOutcome> steps, Map<String, Integer> skipped, List<ScenarioExecutionReport.Blocker> blockers, List<ParticipantCandidate> participantCandidates) {
        CatalogScenarioRecord source = record != null ? record : records.stream().findFirst().orElse(null);
        String scenarioPlanId = record == null ? null : record.plan().deterministicId();
        ScenarioExecutionReport.RuntimeMetadata runtimeMetadata = new ScenarioExecutionReport.RuntimeMetadata(
                options.applicationBase(),
                options.applicationId(),
                options.springApplicationClass(),
                options.springProfiles(),
                options.mavenProfile(),
                source == null ? null : source.catalogPath(),
                source == null ? null : source.catalogKind(),
                scenarioPlanId,
                vectorSource,
                mode,
                options.dryRun());
        List<ScenarioExecutionReport.Participant> participants = participantCandidates.stream()
                .map(participant -> participantEntry(participant, status, lifecycleOutcome, steps, slots, blockers))
                .toList();
        return new ScenarioExecutionReport(ScenarioExecutionReport.SCHEMA_VERSION, scenarioExecutionId, status,
                source == null ? null : source.catalogPath(), source == null ? null : source.catalogKind(), mode, reason, requested,
                scenarioPlanId, record == null ? null : record.plan().kind().name(), assignedVector, vectorSource, providerMode, runtimeMetadata, slots, skipped, blockers, participants);
    }

    private ScenarioExecutionReport.Participant participantEntry(ParticipantCandidate participant, String status, String lifecycleOutcome, List<ScenarioExecutionReport.StepOutcome> steps, List<ScenarioExecutionReport.FaultSlot> slots, List<ScenarioExecutionReport.Blocker> blockers) {
        String sagaInstanceId = participant.saga().deterministicId();
        List<ScenarioExecutionReport.StepOutcome> participantSteps = steps.stream()
                .filter(step -> slots.stream().anyMatch(slot -> slot.scheduledStepId().equals(step.scheduledStepId()) && Objects.equals(slot.sagaInstanceId(), sagaInstanceId))
                        || steps.size() == 1 && slots.isEmpty())
                .toList();
        List<ScenarioExecutionReport.FaultSlot> participantSlots = slots.stream()
                .filter(slot -> Objects.equals(slot.sagaInstanceId(), sagaInstanceId))
                .toList();
        return new ScenarioExecutionReport.Participant(
                sagaInstanceId,
                participant.saga().sagaFqn(),
                participant.input() == null ? null : participant.input().deterministicId(),
                materializationState(status),
                startupState(status),
                lifecycleOutcome,
                participantSteps,
                skippedSteps(participantSteps, participantSlots),
                blockers);
    }

    private String materializationState(String status) {
        return switch (status) {
            case "DRY_RUN", "SELECTION_FAILED", "UNSUPPORTED_SCENARIO", "INVALID_FAULT_VECTOR" -> "NOT_ATTEMPTED";
            case "MATERIALIZATION_FAILED" -> "MATERIALIZATION_FAILED";
            default -> "MATERIALIZED";
        };
    }

    private String startupState(String status) {
        return switch (status) {
            case "DRY_RUN", "SELECTION_FAILED", "UNSUPPORTED_SCENARIO", "INVALID_FAULT_VECTOR", "MATERIALIZATION_FAILED" -> "NOT_ATTEMPTED";
            case "STARTUP_FAILED" -> "STARTUP_FAILED";
            default -> "STARTUP_READY";
        };
    }

    private List<ScenarioExecutionReport.SkippedStep> skippedSteps(List<ScenarioExecutionReport.StepOutcome> steps, List<ScenarioExecutionReport.FaultSlot> slots) {
        Set<String> executed = new HashSet<>();
        for (ScenarioExecutionReport.StepOutcome step : steps) executed.add(step.scheduledStepId());
        return slots.stream()
                .filter(slot -> !executed.contains(slot.scheduledStepId()) && slot.assignedBit() == 1 && "MASKED_BY_SAGA_FAILURE".equals(slot.realizationState()))
                .map(slot -> new ScenarioExecutionReport.SkippedStep(slot.scheduledStepId(), slot.catalogStepId(), slot.scheduleOrder(), slot.runtimeStepName(), "SKIPPED_BY_SAGA_FAILURE"))
                .toList();
    }

    private List<ScenarioExecutionReport.Blocker> withScenario(String planId, List<ScenarioExecutionReport.Blocker> blockers) {
        return blockers.stream().map(blocker -> new ScenarioExecutionReport.Blocker(planId, blocker.inputVariantId(), blocker.argumentIndex(), blocker.scheduledStepId(), blocker.reason(), blocker.message())).toList();
    }

    private void writeReport(ScenarioExecutorOptions options, ScenarioExecutionReport report) {
        if (options.outputPath() == null) return;
        try {
            if (options.outputPath().getParent() != null) Files.createDirectories(options.outputPath().getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(options.outputPath().toFile(), report);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write scenario execution report", e);
        }
    }

    private record Candidate(boolean supported, InputVariant input, List<ParticipantCandidate> participants, List<RuntimeStep> steps, AssignedVector vector, List<ScenarioExecutionReport.FaultSlot> faultSlots, List<ScenarioExecutionReport.Blocker> blockers) {}
    private static final class ParticipantRuntimeState {
        private final SagaInstance saga;
        private final InputVariant input;
        private String materializationState = "NOT_ATTEMPTED";
        private String startupState = "NOT_ATTEMPTED";
        private String lifecycleOutcome = "NOT_STARTED";
        private List<Object> materializedArguments = List.of();
        private Object functionality;
        private Object unitOfWork;
        private Integer lastExecutedScheduleOrder;
        private final List<ScenarioExecutionReport.StepOutcome> stepOutcomes = new ArrayList<>();
        private final List<ScenarioExecutionReport.SkippedStep> skippedSteps = new ArrayList<>();
        private final List<ScenarioExecutionReport.Blocker> blockers = new ArrayList<>();

        private ParticipantRuntimeState(SagaInstance saga, InputVariant input) {
            this.saga = saga;
            this.input = input;
        }
    }
    private record ParticipantCandidate(SagaInstance saga, InputVariant input) {}
    private record RuntimeStep(ScheduledStep scheduled, String runtimeName) {}
    private record AssignedVector(String value, String source, List<ScenarioExecutionReport.FaultSlot> slots) {}
    private record ClosureResult(boolean failed, String lifecycleOutcome, List<ScenarioExecutionReport.Blocker> blockers) {}
}
