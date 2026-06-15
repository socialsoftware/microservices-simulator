package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;

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
        ScenarioExecutionReport report = options.scenarioId() == null || options.scenarioId().isBlank()
                ? autoSelect(records, options, runtimeContext)
                : explicit(records, options, runtimeContext);
        writeReport(options, report);
        return report;
    }

    private ScenarioExecutionReport explicit(List<CatalogScenarioRecord> records, ScenarioExecutorOptions options, ScenarioRuntimeContext runtimeContext) {
        CatalogScenarioRecord record = records.stream()
                .filter(candidate -> options.scenarioId().equals(candidate.plan().deterministicId()))
                .findFirst()
                .orElse(null);
        if (record == null) {
            return base(records, "SELECTION_FAILED", "EXPLICIT", "requested scenario plan id not found", options.scenarioId(), null,
                    List.of(), List.of(new ScenarioExecutionReport.Blocker(options.scenarioId(), null, null, null, "MISSING_SCENARIO_PLAN_ID", options.scenarioId())));
        }
        Candidate candidate = validate(record);
        if (!candidate.supported()) {
            return base(records, "UNSUPPORTED_SCENARIO", "EXPLICIT", "explicit scenario is unsupported", options.scenarioId(), record,
                    List.of(), candidate.blockers());
        }
        return runCandidate(records, record, candidate, options, runtimeContext, "EXPLICIT", "requested scenario plan id");
    }

    private ScenarioExecutionReport autoSelect(List<CatalogScenarioRecord> records, ScenarioExecutorOptions options, ScenarioRuntimeContext runtimeContext) {
        Map<String, Integer> skipped = new TreeMap<>();
        for (CatalogScenarioRecord record : ordered(records)) {
            Candidate candidate = validate(record);
            if (!candidate.supported()) {
                candidate.blockers().forEach(blocker -> skipped.merge(blocker.reason(), 1, Integer::sum));
                continue;
            }
            SagaInstance saga = record.plan().sagaInstances().get(0);
            ScenarioMaterializer.MaterializedArguments args = materializer.materialize(candidate.input(), runtimeContext, saga.sagaFqn());
            if (!args.success()) {
                args.blockers().forEach(blocker -> skipped.merge(blocker.reason(), 1, Integer::sum));
                continue;
            }
            ScenarioExecutionReport report = runCandidate(records, record, candidate, options, runtimeContext, "AUTO", "first materializable single-saga candidate");
            Map<String, Integer> counts = new TreeMap<>(report.skippedCandidateCounts());
            counts.putAll(skipped);
            return new ScenarioExecutionReport(report.schemaVersion(), report.terminalStatus(), report.catalogPath(), report.catalogKind(), report.selectionMode(), report.selectionReason(), report.requestedScenarioPlanId(), report.scenarioPlanId(), report.sagaInstanceId(), report.sagaFqn(), report.inputVariantId(), report.stepOutcomes(), counts, report.blockers());
        }
        return base(records, "UNSUPPORTED_SCENARIO", "AUTO", "no materializable single-saga candidate", null, null, List.of(), List.of());
    }

    private ScenarioExecutionReport runCandidate(List<CatalogScenarioRecord> records,
                                                CatalogScenarioRecord record,
                                                Candidate candidate,
                                                ScenarioExecutorOptions options,
                                                ScenarioRuntimeContext runtimeContext,
                                                String selectionMode,
                                                String selectionReason) {
        ScenarioPlan plan = record.plan();
        SagaInstance saga = plan.sagaInstances().get(0);
        InputVariant input = candidate.input();
        List<ScenarioExecutionReport.StepOutcome> drySteps = candidate.steps().stream()
                .map(step -> new ScenarioExecutionReport.StepOutcome(step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.runtimeName(), "DRY_RUN", null, null))
                .toList();
        if (options.dryRun()) {
            return report(records, "DRY_RUN", selectionMode, selectionReason, options.scenarioId(), record, saga, input, drySteps, Map.of(), List.of());
        }
        ScenarioMaterializer.MaterializedArguments args = materializer.materialize(input, runtimeContext, saga.sagaFqn());
        if (!args.success()) {
            return report(records, "MATERIALIZATION_FAILED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, List.of(), Map.of(), withScenario(plan.deterministicId(), args.blockers()));
        }
        Object functionality;
        Object unitOfWork;
        try {
            Class<?> sagaClass = Class.forName(saga.sagaFqn());
            functionality = instantiate(sagaClass, args.values());
            unitOfWork = runtimeContext.createSagaUnitOfWork(saga.sagaFqn());
        } catch (ReflectiveOperationException | RuntimeException e) {
            return report(records, "STARTUP_FAILED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, List.of(), Map.of(),
                    List.of(new ScenarioExecutionReport.Blocker(plan.deterministicId(), input.deterministicId(), null, null, "STARTUP_FAILED", e.getMessage())));
        }
        List<ScenarioExecutionReport.StepOutcome> outcomes = new ArrayList<>();
        for (RuntimeStep step : candidate.steps()) {
            try {
                Method method = functionality.getClass().getMethod("executeUntilStep", String.class, Class.forName("pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork"));
                method.invoke(functionality, step.runtimeName(), unitOfWork);
                outcomes.add(new ScenarioExecutionReport.StepOutcome(step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.runtimeName(), "COMPLETED", null, null));
            } catch (ReflectiveOperationException | RuntimeException e) {
                Throwable failure = e.getCause() == null ? e : e.getCause();
                outcomes.add(new ScenarioExecutionReport.StepOutcome(step.scheduled().deterministicId(), step.scheduled().stepId(), step.scheduled().scheduleOrder(), step.runtimeName(), "FAILED", failure.getClass().getName(), failure.getMessage()));
                return report(records, "STEP_EXECUTION_FAILED", selectionMode, selectionReason, options.scenarioId(), record, saga, input, outcomes, Map.of(), List.of());
            }
        }
        return report(records, "SUCCESS", selectionMode, selectionReason, options.scenarioId(), record, saga, input, outcomes, Map.of(), List.of());
    }

    private Candidate validate(CatalogScenarioRecord record) {
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
        return new Candidate(blockers.isEmpty(), input, steps, blockers);
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

    private ScenarioExecutionReport base(List<CatalogScenarioRecord> records, String status, String mode, String reason, String requested, CatalogScenarioRecord record, List<ScenarioExecutionReport.StepOutcome> steps, List<ScenarioExecutionReport.Blocker> blockers) {
        return report(records, status, mode, reason, requested, record, null, null, steps, Map.of(), blockers);
    }

    private ScenarioExecutionReport report(List<CatalogScenarioRecord> records, String status, String mode, String reason, String requested, CatalogScenarioRecord record, SagaInstance saga, InputVariant input, List<ScenarioExecutionReport.StepOutcome> steps, Map<String, Integer> skipped, List<ScenarioExecutionReport.Blocker> blockers) {
        CatalogScenarioRecord source = record != null ? record : records.stream().findFirst().orElse(null);
        return new ScenarioExecutionReport(ScenarioExecutionReport.SCHEMA_VERSION, status,
                source == null ? null : source.catalogPath(), source == null ? null : source.catalogKind(), mode, reason, requested,
                record == null ? null : record.plan().deterministicId(), saga == null ? null : saga.deterministicId(), saga == null ? null : saga.sagaFqn(), input == null ? null : input.deterministicId(), steps, skipped, blockers);
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

    private record Candidate(boolean supported, InputVariant input, List<RuntimeStep> steps, List<ScenarioExecutionReport.Blocker> blockers) {}
    private record RuntimeStep(ScheduledStep scheduled, String runtimeName) {}
}
