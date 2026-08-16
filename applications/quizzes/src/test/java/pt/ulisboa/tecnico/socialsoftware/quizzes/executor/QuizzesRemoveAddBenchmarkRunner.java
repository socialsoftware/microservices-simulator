package pt.ulisboa.tecnico.socialsoftware.quizzes.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventReplayCoordinator;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregateRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutionReport;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorOptions;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioImpactReport;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioReportWriteException;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioRuntimeContext;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogPackageReader;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationCheckpoint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenario;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioAction;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ForwardFaultSlot;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.sagas.SagaQuiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.SagaTournament;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class QuizzesRemoveAddBenchmarkRunner {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SPRING_PROFILES = "test,sagas,local";
    private static final String REMOVE_SAGA =
            "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveTournamentFunctionalitySagas";
    private static final String ADD_SAGA =
            "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas";
    private static final List<String> FORWARD_ORDER = List.of(
            "getTournamentStep", "removeQuizStep", "removeTournamentStep", "getUserStep", "addParticipantStep");

    private QuizzesRemoveAddBenchmarkRunner() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> options = parse(args);
        Path resultPath = requiredPath(options, "result-output-path");
        QuizzesRemoveAddBenchmarkAttempt result;
        try {
            result = run(options);
        } catch (UnsafeResultPathException failure) {
            throw failure;
        } catch (Throwable failure) {
            result = notEvaluated(options, null, null, "RUNNER_FAILED", failureDetails(failure));
        }
        writeResult(resultPath, result);
        System.out.println("Quizzes RemoveTournament-AddParticipant benchmark attempt "
                + result.executionAttemptId() + " classification=" + result.classification()
                + " validity=" + result.validity());
    }

    static QuizzesRemoveAddBenchmarkAttempt run(Map<String, String> options) throws Exception {
        Invocation invocation = validate(options);
        PackageSelection selection;
        try {
            selection = select(invocation);
        } catch (UnsafeResultPathException failure) {
            throw failure;
        } catch (Throwable failure) {
            return notEvaluated(options, invocation, null, "SELECTION_FAILED", failureDetails(failure));
        }

        ScenarioExecutionReport execution = null;
        ScenarioImpactReport impact = null;
        System.setProperty("spring.profiles.active", SPRING_PROFILES);
        System.setProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY, "true");
        try (EventReplayCoordinator.Activation ignored = EventReplayCoordinator.activate()) {
            SpringApplication application = new SpringApplication(QuizzesSimulator.class);
            application.setDefaultProperties(Map.of(
                    "server.port", "0",
                    "verifiers.application.enabled", "false"));
            try (ConfigurableApplicationContext context = application.run()) {
                ScenarioExecutorOptions executorOptions = new ScenarioExecutorOptions(
                        invocation.manifestPath(), invocation.executionOutputPath(), invocation.faultScenarioId(), false,
                        "quizzes", "quizzes", QuizzesSimulator.class.getName(), SPRING_PROFILES,
                        "test-sagas", invocation.impactOutputPath());
                try {
                    execution = new ScenarioExecutor().execute(executorOptions, new SpringRuntimeContext(context));
                } catch (ScenarioReportWriteException reportFailure) {
                    execution = reportFailure.report();
                    return notEvaluated(options, invocation, execution,
                            "EXECUTION_REPORT_WRITE_FAILED", failureDetails(reportFailure));
                }
                impact = MAPPER.readValue(invocation.impactOutputPath().toFile(), ScenarioImpactReport.class);
                validateJoins(selection, execution, impact);
                return observe(options, invocation, selection, execution, impact,
                        context.getBean(SagaAggregateRepository.class),
                        new QuizzesPersistedSagaStateObserver(context.getBean(DataSource.class)));
            }
        } catch (Throwable failure) {
            return notEvaluated(options, invocation, execution, "EXECUTION_OR_OBSERVATION_FAILED",
                    failureDetails(failure), impact, selection);
        } finally {
            System.clearProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY);
            System.clearProperty("spring.profiles.active");
        }
    }

    private static PackageSelection select(Invocation invocation) {
        ScenarioCatalogPackageReader.PackageContents contents =
                new ScenarioCatalogPackageReader().read(invocation.manifestPath());
        rejectResultAlias(invocation, contents);
        WorkloadPlan workload = contents.workloadPlans().stream()
                .filter(candidate -> Objects.equals(candidate.deterministicId(), invocation.workloadPlanId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing selected WorkloadPlan "
                        + invocation.workloadPlanId()));
        FaultScenario scenario = contents.faultScenarios().stream()
                .filter(candidate -> Objects.equals(candidate.deterministicId(), invocation.faultScenarioId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing selected FaultScenario "
                        + invocation.faultScenarioId()));
        if (!Objects.equals(scenario.workloadPlanId(), workload.deterministicId())) {
            throw new IllegalArgumentException("Selected FaultScenario belongs to another WorkloadPlan");
        }
        Set<String> participantTypes = workload.participants().stream()
                .map(participant -> participant.sagaFqn())
                .collect(java.util.stream.Collectors.toSet());
        if (!participantTypes.equals(Set.of(REMOVE_SAGA, ADD_SAGA))
                || !workload.forwardSchedule().stream().map(step -> step.runtimeStepName()).toList().equals(FORWARD_ORDER)
                || !workload.eventConsequences().isEmpty()
                || !workload.faultSlots().stream().map(slot -> slot.runtimeStepName()).toList().equals(FORWARD_ORDER)
                || workload.prerequisiteBaseline() == null
                || !QuizzesStaleReadPrerequisiteProvider.PROVIDER_ID.equals(
                workload.prerequisiteBaseline().providerId())) {
            throw new IllegalArgumentException("Selected WorkloadPlan is not the exact RemoveTournament-AddParticipant benchmark");
        }
        return new PackageSelection(workload, scenario, persistedActions(workload, scenario));
    }

    private static List<QuizzesRemoveAddBenchmarkAttempt.PersistedAction> persistedActions(
            WorkloadPlan workload, FaultScenario scenario) {
        Map<String, ForwardFaultSlot> slots = new LinkedHashMap<>();
        workload.faultSlots().forEach(slot -> slots.put(slot.deterministicId(), slot));
        Map<String, CompensationCheckpoint> checkpoints = new LinkedHashMap<>();
        workload.compensationCheckpoints().forEach(checkpoint ->
                checkpoints.put(checkpoint.deterministicId(), checkpoint));
        List<QuizzesRemoveAddBenchmarkAttempt.PersistedAction> actions = new ArrayList<>();
        for (int position = 0; position < scenario.actions().size(); position++) {
            FaultScenarioAction action = scenario.actions().get(position);
            ForwardFaultSlot slot = slots.get(action.sourceFaultSlotId());
            CompensationCheckpoint checkpoint = checkpoints.get(action.sourceCompensationCheckpointId());
            actions.add(new QuizzesRemoveAddBenchmarkAttempt.PersistedAction(
                    position,
                    action.deterministicId(),
                    action.kind().name(),
                    action.sagaInstanceId(),
                    slot != null ? slot.runtimeStepName() : checkpoint == null ? null : checkpoint.runtimeStepName(),
                    checkpoint == null ? null : checkpoint.evidenceClass().name()));
        }
        return List.copyOf(actions);
    }

    private static void validateJoins(PackageSelection selection,
                                      ScenarioExecutionReport execution,
                                      ScenarioImpactReport impact) {
        if (!Objects.equals(execution.workloadPlanId(), selection.workload().deterministicId())
                || !Objects.equals(execution.faultScenarioId(), selection.scenario().deterministicId())
                || !Objects.equals(execution.assignedVector(), selection.scenario().assignedVector())
                || !execution.plannedActions().stream().map(ScenarioExecutionReport.PlannedAction::actionId).toList()
                .equals(selection.scenario().actions().stream().map(FaultScenarioAction::deterministicId).toList())) {
            throw new IllegalArgumentException("Execution report does not match the persisted package selection");
        }
        if (!Objects.equals(impact.executionAttemptId(), execution.executionAttemptId())
                || !Objects.equals(impact.workloadPlanId(), execution.workloadPlanId())
                || !Objects.equals(impact.faultScenarioId(), execution.faultScenarioId())) {
            throw new IllegalArgumentException("ImpactV1 report does not match the execution attempt");
        }
    }

    private static QuizzesRemoveAddBenchmarkAttempt observe(
            Map<String, String> options,
            Invocation invocation,
            PackageSelection selection,
            ScenarioExecutionReport execution,
            ScenarioImpactReport impact,
            SagaAggregateRepository repository,
            QuizzesPersistedSagaStateObserver sagaStateObserver) {
        if (!Set.of("SUCCESS", "COMPENSATED", "PARTIAL_COMPENSATED").contains(execution.terminalStatus())
                || !"EVALUATED".equals(impact.evaluationStatus())) {
            return notEvaluated(options, invocation, execution, "EXECUTION_NOT_IMPACT_EVALUABLE",
                    execution.terminalStatus(), impact, selection);
        }
        Map<String, String> evidence = execution.prerequisiteSetup() == null
                ? Map.of() : execution.prerequisiteSetup().evidence();
        int tournamentId = exactId(evidence, "tournamentAggregateId");
        int quizId = exactId(evidence, "referencedQuizAggregateId");
        Aggregate tournamentAggregate = repository.findAnySagaAggregate(tournamentId)
                .orElseThrow(() -> new IllegalStateException("Tournament observation found no aggregate " + tournamentId));
        Aggregate quizAggregate = repository.findAnySagaAggregate(quizId)
                .orElseThrow(() -> new IllegalStateException("Quiz observation found no aggregate " + quizId));
        if (!(tournamentAggregate instanceof SagaTournament tournament)
                || !(quizAggregate instanceof SagaQuiz quiz)) {
            throw new IllegalStateException("Observed aggregate types do not match Tournament and Quiz identities");
        }
        QuizzesPersistedSagaStateObserver.PersistedSagaState persistedSagaState =
                sagaStateObserver.observeTournament(tournamentId);
        Integer referencedQuizId = tournament.getTournamentQuiz().getQuizAggregateId();
        if (!isOutsideActiveSaga(persistedSagaState)) {
            return attempt(options, invocation, selection, execution, impact,
                    classify(false, false, false), "INVALID",
                    "TOURNAMENT_PERSISTED_SAGA_STATE_MISMATCH: expected decoded "
                            + GenericSagaState.class.getName() + ":" + GenericSagaState.NOT_IN_SAGA.name()
                            + " but latest raw evidence was valueStatus=" + persistedSagaState.valueStatus()
                            + ", decodeStatus=" + persistedSagaState.decodeStatus()
                            + (persistedSagaState.rawValue() == null
                            ? "" : ", rawValue=" + persistedSagaState.rawValue()),
                    observed(tournament, referencedQuizId, persistedSagaState), observed(quiz, null, null),
                    false, null, "final-state rule was not evaluated because outside-active-Saga proof failed");
        }
        boolean broken = isBrokenReference(
                tournament.getState(), referencedQuizId, quizId, quiz.getState());
        String classification = classify(true, true, broken);
        String reason = broken
                ? "active Tournament refers to the observed deleted Quiz"
                : "the benchmark broken-reference predicate is false; this is not a global safety claim";
        return attempt(options, invocation, selection, execution, impact, classification, "VALID", null,
                observed(tournament, referencedQuizId, persistedSagaState), observed(quiz, null, null),
                true, broken, reason);
    }

    public static boolean isOutsideActiveSaga(
            QuizzesPersistedSagaStateObserver.PersistedSagaState persistedSagaState) {
        return persistedSagaState != null
                && QuizzesPersistedSagaStateObserver.VALUE.equals(persistedSagaState.valueStatus())
                && QuizzesPersistedSagaStateObserver.DECODED.equals(persistedSagaState.decodeStatus())
                && GenericSagaState.class.getName().equals(persistedSagaState.decodedStateClass())
                && GenericSagaState.NOT_IN_SAGA.name().equals(persistedSagaState.decodedStateName());
    }

    public static boolean isBrokenReference(Aggregate.AggregateState tournamentState,
                                     Integer tournamentQuizId,
                                     Integer observedQuizId,
                                     Aggregate.AggregateState quizState) {
        return tournamentState == Aggregate.AggregateState.ACTIVE
                && Objects.equals(tournamentQuizId, observedQuizId)
                && quizState == Aggregate.AggregateState.DELETED;
    }

    public static String classify(boolean impactEvaluable,
                           boolean observationCompleted,
                           boolean brokenReference) {
        if (!impactEvaluable || !observationCompleted) return "NOT_EVALUATED";
        return brokenReference ? "HARMFUL_FOR_RULE" : "NO_BROKEN_REFERENCE";
    }

    private static QuizzesRemoveAddBenchmarkAttempt.ObservedAggregate observed(
            Aggregate aggregate,
            Integer referencedQuizId,
            QuizzesPersistedSagaStateObserver.PersistedSagaState persistedSagaState) {
        return new QuizzesRemoveAddBenchmarkAttempt.ObservedAggregate(
                aggregate.getAggregateId(), aggregate.getAggregateType(), aggregate.getState().name(),
                persistedSagaState, referencedQuizId);
    }

    private static QuizzesRemoveAddBenchmarkAttempt notEvaluated(
            Map<String, String> options,
            Invocation invocation,
            ScenarioExecutionReport execution,
            String reason,
            String details) {
        return notEvaluated(options, invocation, execution, reason, details, null, null);
    }

    private static QuizzesRemoveAddBenchmarkAttempt notEvaluated(
            Map<String, String> options,
            Invocation invocation,
            ScenarioExecutionReport execution,
            String reason,
            String details,
            ScenarioImpactReport impact,
            PackageSelection selection) {
        Invocation effective = invocation == null ? Invocation.partial(options) : invocation;
        return attempt(options, effective, selection, execution, impact,
                classify(false, false, false), "INVALID",
                reason + (details == null ? "" : ": " + details), null, null, false, null,
                "final-state rule was not evaluated");
    }

    private static QuizzesRemoveAddBenchmarkAttempt attempt(
            Map<String, String> options,
            Invocation invocation,
            PackageSelection selection,
            ScenarioExecutionReport execution,
            ScenarioImpactReport impact,
            String classification,
            String validity,
            String validityReason,
            QuizzesRemoveAddBenchmarkAttempt.ObservedAggregate tournament,
            QuizzesRemoveAddBenchmarkAttempt.ObservedAggregate quiz,
            boolean observationCompleted,
            Boolean brokenReference,
            String brokenReferenceReason) {
        List<QuizzesRemoveAddBenchmarkAttempt.PersistedAction> actions = selection == null
                ? List.of() : selection.actions();
        String vector = selection == null ? execution == null ? null : execution.assignedVector()
                : selection.scenario().assignedVector();
        return new QuizzesRemoveAddBenchmarkAttempt(
                null, null, null, classification, validity, validityReason,
                invocation.manifestPath() == null ? null : invocation.manifestPath().toString(),
                sha256IfPresent(invocation.manifestPath()),
                invocation.workloadPlanId(), invocation.faultScenarioId(),
                execution == null ? null : execution.executionAttemptId(), vector, actions,
                invocation.faultScenarioId(),
                execution == null ? null : execution.terminalStatus(),
                execution == null ? null : execution.scheduleConformance(),
                impact == null ? null : new QuizzesRemoveAddBenchmarkAttempt.ImpactV1(
                        impact.evaluationStatus(), impact.notEvaluatedReason(),
                        impact.invariantViolationCount(), impact.impactScore()),
                tournament, quiz, observationCompleted, brokenReference, brokenReferenceReason,
                invocation.repetition(), runtime(options, invocation.runtimeContextId()));
    }

    private static QuizzesRemoveAddBenchmarkAttempt.RuntimeContext runtime(
            Map<String, String> options, String runtimeContextId) {
        return new QuizzesRemoveAddBenchmarkAttempt.RuntimeContext(
                "FRESH_PROCESS_AND_H2",
                runtimeContextId,
                ProcessHandle.current().pid(),
                "H2_IN_MEMORY",
                SPRING_PROFILES,
                System.getProperty("java.version"),
                options.get("source-revision"),
                options.get("source-tree-state"));
    }

    private static int exactId(Map<String, String> evidence, String key) {
        String value = evidence.get(key);
        if (value == null || !value.matches("[1-9][0-9]*")) {
            throw new IllegalArgumentException("Missing exact prerequisite evidence " + key);
        }
        return Integer.parseInt(value);
    }

    private static Invocation validate(Map<String, String> options) {
        Set<String> supported = Set.of(
                "package-path", "workload-plan-id", "fault-scenario-id", "execution-output-path",
                "impact-output-path", "result-output-path", "repetition", "runtime-context-id",
                "source-revision", "source-tree-state");
        options.keySet().stream().filter(key -> !supported.contains(key)).findFirst()
                .ifPresent(key -> { throw new IllegalArgumentException("Unsupported benchmark option --" + key); });
        int repetition;
        try {
            repetition = Integer.parseInt(required(options, "repetition"));
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("--repetition must be a positive integer", failure);
        }
        if (repetition < 1) throw new IllegalArgumentException("--repetition must be a positive integer");
        required(options, "source-revision");
        required(options, "source-tree-state");
        return new Invocation(
                requiredPath(options, "package-path"),
                required(options, "workload-plan-id"),
                required(options, "fault-scenario-id"),
                requiredPath(options, "execution-output-path"),
                requiredPath(options, "impact-output-path"),
                requiredPath(options, "result-output-path"),
                repetition,
                required(options, "runtime-context-id"));
    }

    private static void rejectResultAlias(Invocation invocation,
                                          ScenarioCatalogPackageReader.PackageContents contents) {
        List<Path> inputs = List.of(
                invocation.manifestPath(), contents.workloadCatalogPath(), contents.faultScenarioCatalogPath(),
                contents.accountingPath(), contents.rejectedInputsPath(), invocation.executionOutputPath(),
                invocation.impactOutputPath());
        for (Path input : inputs) {
            if (samePath(invocation.resultOutputPath(), input)) {
                throw new UnsafeResultPathException(
                        "Benchmark result output must not alias package or executor artifacts");
            }
        }
    }

    private static boolean samePath(Path first, Path second) {
        Path left = first.toAbsolutePath().normalize();
        Path right = second.toAbsolutePath().normalize();
        if (left.equals(right)) return true;
        try {
            return Files.exists(left) && Files.exists(right) && Files.isSameFile(left, right);
        } catch (IOException failure) {
            throw new IllegalArgumentException("Cannot compare benchmark artifact paths", failure);
        }
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> parsed = new LinkedHashMap<>();
        for (int index = 0; index < args.length; index++) {
            if (!args[index].startsWith("--")) continue;
            String key = args[index].substring(2);
            if (index + 1 >= args.length || args[index + 1].startsWith("--")) {
                throw new IllegalArgumentException("--" + key + " requires a value");
            }
            parsed.put(key, args[++index]);
        }
        return parsed;
    }

    private static String required(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing required --" + key);
        return value;
    }

    private static Path requiredPath(Map<String, String> options, String key) {
        return Path.of(required(options, key)).toAbsolutePath().normalize();
    }

    private static String sha256IfPresent(Path path) {
        if (path == null || !Files.isRegularFile(path)) return null;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
        } catch (IOException | NoSuchAlgorithmException failure) {
            throw new IllegalStateException("Cannot hash package manifest", failure);
        }
    }

    private static void writeResult(Path output, QuizzesRemoveAddBenchmarkAttempt result) throws IOException {
        Path parent = output.getParent();
        if (parent != null) Files.createDirectories(parent);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), result);
    }

    private static String failureDetails(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null && current != current.getCause()) current = current.getCause();
        return current.getClass().getName() + (current.getMessage() == null ? "" : ": " + current.getMessage());
    }

    record Invocation(
            Path manifestPath,
            String workloadPlanId,
            String faultScenarioId,
            Path executionOutputPath,
            Path impactOutputPath,
            Path resultOutputPath,
            int repetition,
            String runtimeContextId) {
        private static Invocation partial(Map<String, String> options) {
            return new Invocation(
                    optionalPath(options.get("package-path")),
                    options.get("workload-plan-id"),
                    options.get("fault-scenario-id"),
                    optionalPath(options.get("execution-output-path")),
                    optionalPath(options.get("impact-output-path")),
                    optionalPath(options.get("result-output-path")),
                    parseRepetition(options.get("repetition")),
                    options.get("runtime-context-id"));
        }

        private static Path optionalPath(String value) {
            return value == null || value.isBlank() ? null : Path.of(value).toAbsolutePath().normalize();
        }

        private static int parseRepetition(String value) {
            try {
                return value == null ? 0 : Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }

    private record PackageSelection(
            WorkloadPlan workload,
            FaultScenario scenario,
            List<QuizzesRemoveAddBenchmarkAttempt.PersistedAction> actions) {
    }

    private static final class UnsafeResultPathException extends IllegalArgumentException {
        private UnsafeResultPathException(String message) {
            super(message);
        }
    }

    private record SpringRuntimeContext(ConfigurableApplicationContext context) implements ScenarioRuntimeContext {
        @Override
        public Object bean(Class<?> type) {
            return context.getBean(type);
        }

        @Override
        public <T> List<T> beans(Class<T> type) {
            return List.copyOf(context.getBeansOfType(type).values());
        }
    }
}
