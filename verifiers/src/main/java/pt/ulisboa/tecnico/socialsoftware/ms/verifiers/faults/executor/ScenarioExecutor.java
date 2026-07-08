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
        Candidate candidate = validate(record, options);
        if (!candidate.supported()) {
            boolean invalidVector = candidate.blockers().stream().anyMatch(this::isVectorBlocker);
            String status = invalidVector ? "INVALID_FAULT_VECTOR" : "UNSUPPORTED_SCENARIO";
            String reason = invalidVector ? "explicit scenario has invalid fault vector" : "explicit scenario is unsupported";
            return base(records, scenarioExecutionId, status, "EXPLICIT", reason, options.scenarioId(), record,
                    attemptedVector(record, options), null, vectorSource(options), options, candidate.faultSlots(), candidate.blockers());
        }
        return runCandidate(records, record, candidate, options, runtimeContext, scenarioExecutionId, "EXPLICIT", "requested scenario plan id");
    }

    private ScenarioExecutionReport autoSelect(List<CatalogScenarioRecord> records, ScenarioExecutorOptions options, ScenarioRuntimeContext runtimeContext, String scenarioExecutionId) {
        Map<String, Integer> skipped = new TreeMap<>();
        for (CatalogScenarioRecord record : ordered(records)) {
            Candidate candidate = validate(record, options);
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
            return new ScenarioExecutionReport(report.schemaVersion(), report.scenarioExecutionId(), report.terminalStatus(), report.lifecycleOutcome(), report.catalogPath(), report.catalogKind(), report.selectionMode(), report.selectionReason(), report.requestedScenarioPlanId(), report.scenarioPlanId(), report.sagaInstanceId(), report.sagaFqn(), report.inputVariantId(), report.assignedVector(), report.vectorSource(), report.providerMode(), report.runtimeMetadata(), report.faultSlots(), report.stepOutcomes(), counts, report.blockers());
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
            return report(records, scenarioExecutionId, "DRY_RUN", "NOT_STARTED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, candidate.vector().value(), candidate.vector().source(), "NONE", options, candidate.faultSlots(), drySteps, Map.of(), List.of());
        }
        ScenarioMaterializer.MaterializedArguments args = materializer.materialize(input, runtimeContext, saga.sagaFqn());
        if (!args.success()) {
            return report(records, scenarioExecutionId, "MATERIALIZATION_FAILED", "NOT_STARTED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, candidate.vector().value(), candidate.vector().source(), "NONE", options, candidate.faultSlots(), List.of(), Map.of(), withScenario(plan.deterministicId(), args.blockers()));
        }
        Object functionality;
        Object unitOfWork;
        try {
            Class<?> sagaClass = Class.forName(saga.sagaFqn());
            functionality = instantiate(sagaClass, args.values());
            unitOfWork = runtimeContext.createSagaUnitOfWork(saga.sagaFqn());
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
                            return report(records, scenarioExecutionId, "FAULT_COMPENSATED", closure.lifecycleOutcome(), selectionMode, selectionReason, options.scenarioId(), record, saga, input, candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, outcomes, Map.of(), List.of());
                        }
                        String status = currentSlot.assignedBit() == 0 ? "UNEXPECTED_INJECTED_FAULT" : "FAULT_PROVIDER_MISMATCH";
                        return report(records, scenarioExecutionId, status, "CLOSURE_SKIPPED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, outcomes, Map.of(),
                                List.of(new ScenarioExecutionReport.Blocker(plan.deterministicId(), input.deterministicId(), null, step.scheduled().deterministicId(), status, failureDetails(failure))));
                    }
                    outcomes.add(new ScenarioExecutionReport.StepOutcome(step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.runtimeName(), "FAILED", failure.getClass().getName(), failure.getMessage()));
                    ClosureResult closure = compensate(functionality, unitOfWork, plan, input, step, failure);
                    if (closure.failed()) {
                        return report(records, scenarioExecutionId, "COMPENSATION_FAILED", "COMPENSATION_FAILED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, outcomes, Map.of(), closure.blockers());
                    }
                    return report(records, scenarioExecutionId, "UNEXPECTED_EXECUTION_FAILURE", closure.lifecycleOutcome(), selectionMode, selectionReason, options.scenarioId(), record, saga, input, candidate.vector().value(), candidate.vector().source(), "IN_MEMORY_FAULT_VECTOR", options, faultSlots, outcomes, Map.of(),
                            List.of(new ScenarioExecutionReport.Blocker(plan.deterministicId(), input.deterministicId(), null, step.scheduled().deterministicId(), "UNEXPECTED_EXECUTION_FAILURE", failureDetails(failure))));
                }
                ScenarioExecutionReport.FaultSlot slot = stepSlot(candidate.faultSlots(), step);
                if (slot.assignedBit() == 1) {
                    outcomes.add(new ScenarioExecutionReport.StepOutcome(step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.runtimeName(), "UNREALIZED", null, null));
                    faultSlots = markUnrealized(candidate.faultSlots(), slot.slotIndex());
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

    private boolean matchesCurrentSlot(FaultVectorInjectedFaultException injected, ScenarioExecutionReport.FaultSlot slot) {
        return injected.getScheduledStepId().equals(slot.scheduledStepId())
                && injected.getRuntimeStepName().equals(slot.runtimeStepName())
                && injected.getSlotIndex() == slot.slotIndex()
                && injected.getAssignedBit() == slot.assignedBit()
                && slot.assignedBit() == 1;
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

    private List<ScenarioExecutionReport.FaultSlot> markUnrealized(List<ScenarioExecutionReport.FaultSlot> slots, int unrealizedSlotIndex) {
        List<ScenarioExecutionReport.FaultSlot> updated = new ArrayList<>();
        for (ScenarioExecutionReport.FaultSlot slot : slots) {
            if (slot.slotIndex() == unrealizedSlotIndex) {
                updated.add(new ScenarioExecutionReport.FaultSlot(slot.slotIndex(), slot.scheduledStepId(), slot.catalogStepId(), slot.scheduleOrder(), slot.sagaInstanceId(), slot.runtimeStepName(), slot.assignedBit(), "UNREALIZED", null));
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
        List<ScenarioExecutionReport.FaultSlot> updated = new ArrayList<>();
        for (ScenarioExecutionReport.FaultSlot slot : slots) {
            if (slot.slotIndex() == realizedSlotIndex) {
                updated.add(new ScenarioExecutionReport.FaultSlot(slot.slotIndex(), slot.scheduledStepId(), slot.catalogStepId(), slot.scheduleOrder(), slot.sagaInstanceId(), slot.runtimeStepName(), slot.assignedBit(), "REALIZED", null));
            } else if (slot.slotIndex() > realizedSlotIndex && slot.assignedBit() == 1) {
                updated.add(new ScenarioExecutionReport.FaultSlot(slot.slotIndex(), slot.scheduledStepId(), slot.catalogStepId(), slot.scheduleOrder(), slot.sagaInstanceId(), slot.runtimeStepName(), slot.assignedBit(), "MASKED", "masked by earlier realized slot " + realizedSlotIndex + " after saga abort"));
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

    private Candidate validate(CatalogScenarioRecord record, ScenarioExecutorOptions options) {
        ScenarioPlan plan = record.plan();
        List<ScenarioExecutionReport.Blocker> blockers = new ArrayList<>();
        if (plan.kind() != ScenarioKind.SINGLE_SAGA || plan.sagaInstances().size() != 1) {
            blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), null, null, null, "UNSUPPORTED_SCENARIO_SHAPE", "only exactly-one-instance single-saga plans are supported"));
        }
        SagaInstance saga = plan.sagaInstances().size() == 1 ? plan.sagaInstances().get(0) : null;
        InputVariant input = saga == null ? null : plan.inputs().stream().filter(candidate -> saga.inputVariantId().equals(candidate.deterministicId())).findFirst().orElse(null);
        if (saga != null && input == null) {
            blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), saga.inputVariantId(), null, null, "MISSING_INPUT_VARIANT", "single saga instance has no matching input variant"));
        }
        List<RuntimeStep> steps = new ArrayList<>();
        for (ScheduledStep step : plan.expandedSchedule().stream().sorted(Comparator.comparingInt(ScheduledStep::scheduleOrder)).toList()) {
            String runtimeName = runtimeStepName(step.stepId());
            if (runtimeName == null) {
                blockers.add(new ScenarioExecutionReport.Blocker(plan.deterministicId(), input == null ? null : input.deterministicId(), null, step.deterministicId(), "UNSUPPORTED_STEP_ID", step.stepId()));
            } else {
                steps.add(new RuntimeStep(step, runtimeName));
            }
        }
        AssignedVector vector = validateVector(plan, input, steps, options, blockers);
        return new Candidate(blockers.isEmpty(), input, steps, vector, vector == null ? List.of() : vector.slots(), blockers);
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
            slots.add(new ScenarioExecutionReport.FaultSlot(index, step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.scheduled().sagaInstanceId(), step.runtimeName(), bit, bit == 1 ? "UNREALIZED" : "NOT_ASSIGNED", null));
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
        return report(records, scenarioExecutionId, status, lifecycleOutcome == null ? "NOT_STARTED" : lifecycleOutcome, mode, reason, requested, record, null, null, assignedVector, vectorSource, "NONE", options, slots, List.of(), Map.of(), blockers);
    }

    private ScenarioExecutionReport report(List<CatalogScenarioRecord> records, String scenarioExecutionId, String status, String lifecycleOutcome, String mode, String reason, String requested, CatalogScenarioRecord record, SagaInstance saga, InputVariant input, String assignedVector, String vectorSource, String providerMode, ScenarioExecutorOptions options, List<ScenarioExecutionReport.FaultSlot> slots, List<ScenarioExecutionReport.StepOutcome> steps, Map<String, Integer> skipped, List<ScenarioExecutionReport.Blocker> blockers) {
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
        return new ScenarioExecutionReport(ScenarioExecutionReport.SCHEMA_VERSION, scenarioExecutionId, status, lifecycleOutcome,
                source == null ? null : source.catalogPath(), source == null ? null : source.catalogKind(), mode, reason, requested,
                scenarioPlanId, saga == null ? null : saga.deterministicId(), saga == null ? null : saga.sagaFqn(), input == null ? null : input.deterministicId(), assignedVector, vectorSource, providerMode, runtimeMetadata, slots, steps, skipped, blockers);
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

    private record Candidate(boolean supported, InputVariant input, List<RuntimeStep> steps, AssignedVector vector, List<ScenarioExecutionReport.FaultSlot> faultSlots, List<ScenarioExecutionReport.Blocker> blockers) {}
    private record RuntimeStep(ScheduledStep scheduled, String runtimeName) {}
    private record AssignedVector(String value, String source, List<ScenarioExecutionReport.FaultSlot> slots) {}
    private record ClosureResult(boolean failed, String lifecycleOutcome, List<ScenarioExecutionReport.Blocker> blockers) {}
}
