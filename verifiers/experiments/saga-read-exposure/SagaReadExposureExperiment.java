package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openjdk.jol.info.GraphLayout;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.*;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorDomainException;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.*;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.*;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadObservationContext;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadResponseEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventReplayCoordinator;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.StartQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.sagas.SagaQuiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.FindQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.SagaTournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.CreateTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.FindTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

import static pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.SagaReadExposureReport.*;

/**
 * Application-side qualification only. It supplies the actual newly created ID to B,
 * without changing the ordinary executor's package inputs or runtime binding contract.
 * The executor package permits composition with the existing package-private observer;
 * all read/write assessment is performed by the production collector and assessor.
 */
public final class SagaReadExposureExperiment {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private static final LocalDateTime FIXED_CLOCK = LocalDateTime.of(2030, 1, 1, 12, 0);
    private static final List<String> CREATE_PREFIX = List.of("getCourseExecutionStep", "getCreatorStep",
            "getTopicsStep", "findQuestionsByTopicIdsStep", "getCourseExecutionById", "generateQuizStep");
    private static final List<String> CREATE_RECOVERY = List.of("generateQuizStep", "getTopicsStep", "getCourseExecutionStep");
    private static final String EXPLICIT = "EXPLICIT_COMPENSATION";
    private static final String IMPLICIT = "IMPLICIT_SAGA_ROLLBACK";

    private final ConfigurableApplicationContext context;
    private final String caseId;
    private final boolean serialized;
    private final boolean enabled;
    private final String attempt;
    private final String workload;
    private final String scenario;
    private final Path output;
    private final SagaUnitOfWorkService uowService;
    private final LocalCommandGateway gateway;
    private final PersistentStateObserver stateObserver;
    private final EventService eventService;
    private final List<Planned> plan;
    private final SourceContract sources;
    private final Map<String, Actor> actors = new LinkedHashMap<>();
    private final List<ScenarioExecutionReport.ActionOutcome> actions = new ArrayList<>();
    private final List<ScenarioExecutionReport.LifecycleEvent> lifecycle = new ArrayList<>();
    private final List<Map<String, Object>> checks = new ArrayList<>();
    private final List<Map<String, Object>> workflowReads = new ArrayList<>();
    private final Map<String, Object> aggregateProofs = new LinkedHashMap<>();
    private Setup setup;
    private long actionDurationNanos;
    private long setupDurationNanos;
    private long finishDurationNanos;
    private long setupEventsCleared;
    private Throwable runtimeFailure;
    private Integer quizId;
    private Long createdQuizVersion;

    private SagaReadExposureExperiment(ConfigurableApplicationContext context, String caseId, boolean serialized,
                                      boolean enabled, String attempt, Path output) {
        this.context = context; this.caseId = caseId; this.serialized = serialized;
        this.enabled = enabled; this.attempt = attempt; this.output = output;
        this.workload = "controlled-saga-read:" + caseId;
        this.scenario = "controlled-saga-read:" + caseId + ":v1";
        this.uowService = context.getBean(SagaUnitOfWorkService.class);
        this.gateway = context.getBean(LocalCommandGateway.class);
        this.stateObserver = context.getBean(PersistentStateObserver.class);
        this.eventService = context.getBean(EventService.class);
        this.plan = plan(caseId);
        this.sources = sourceContract(plan);
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 5, "usage: SagaReadExposureExperiment CASE SERIALIZE ENABLED ATTEMPT OUTPUT_DIRECTORY");
        require(Set.of("split-start", "early-compensation", "producer-success", "reader-only", "tournament-outer")
                .contains(args[0]), "unknown controlled case " + args[0]);
        boolean serialized = bool(args[1]); boolean enabled = bool(args[2]);
        Path output = Path.of(args[4]).toAbsolutePath().normalize();
        Files.createDirectories(output);
        require(!Files.exists(output.resolve("experiment.json")), "refusing to overwrite a completed run");
        System.setProperty("spring.profiles.active", "test,sagas,local");
        System.setProperty("local.messaging.serialize", String.valueOf(serialized));
        System.setProperty(SagaReadExposureCollector.ENABLED_PROPERTY, String.valueOf(enabled));
        System.setProperty(ImpactV2EvidenceCollector.ENABLED_PROPERTY, "true");
        System.setProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY, "true");
        long totalStart = System.nanoTime();
        // The clock fixture changes no service or DTO source and preserves the actual date converters.
        // Saga/local steps and transaction callbacks execute synchronously on this fixture thread.
        try (MockedStatic<DateHandler> clock = Mockito.mockStatic(DateHandler.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(DateHandler::now).thenReturn(FIXED_CLOCK);
            try (var replay = EventReplayCoordinator.activate();
                 var context = SpringApplication.run(QuizzesSimulator.class,
                         "--verifiers.application.enabled=false", "--server.port=0")) {
                long startupDuration = System.nanoTime() - totalStart;
                new SagaReadExposureExperiment(context, args[0], serialized, enabled, args[3], output)
                        .run(startupDuration, totalStart);
            }
        } catch (Throwable failure) {
            JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve("failure.json").toFile(),
                    map("type", failure.getClass().getName(), "message", failure.getMessage()));
            throw failure;
        } finally {
            FaultVectorProviderHolder.clear();
        }
        System.out.println("SAGA_READ_EXPOSURE_EXPERIMENT_PASS " + args[0] + " serialized=" + serialized + " enabled=" + enabled);
    }

    private void run(long startupDuration, long totalStart) throws Exception {
        long setupStart = System.nanoTime();
        try (var ignored = ReadObservationContext.exclude("SETUP")) {
            setup = setupApplication("tournament-outer".equals(caseId));
            setupEventsCleared = eventService.eventCountForReplay();
            eventService.clearEventsForReplay();
            require(eventService.eventCountForReplay() == 0, "setup did not leave an empty event baseline");
            if (!"tournament-outer".equals(caseId)) actors.put("A", newCreate());
        }
        setupDurationNanos = System.nanoTime() - setupStart;
        check("empty measured pending-event baseline", eventService.eventCountForReplay() == 0, map("cleared", setupEventsCleared));
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve("source-contract.json").toFile(), sources);

        SagaReadExposureCollector diagnostic = enabled ? new SagaReadExposureCollector(attempt, workload, scenario, sources) : null;
        ImpactV2EvidenceCollector impact = new ImpactV2EvidenceCollector(attempt, workload, scenario, diagnostic);
        ImpactV1Collector invariants = new ImpactV1Collector(DynamicEvidenceRecorderHolder.getRecorder(), attempt, workload);
        impact.start(runtimeContext());
        require(impact.started(), "ordinary write observer did not start");
        var impactScope = ImpactEvidenceObserverHolder.install(impact);
        try (var invariantScope = DynamicEvidenceRecorderHolder.install(invariants)) {
            try {
                for (int index = 0; index < plan.size(); index++) execute(plan.get(index), index);
            } catch (Throwable failure) {
                runtimeFailure = failure;
                check("controlled actions complete", false, map("type", failure.getClass().getName(), "message", failure.getMessage()));
            } finally {
                long finishStart = System.nanoTime();
                impact.finish();
                impactScope.close();
                impact.recordObserverFailures(impactScope);
                finishDurationNanos = System.nanoTime() - finishStart;
            }
        }

        ScenarioExecutionReport execution = executionReport();
        ImpactV2EvidenceReport impactReport = impact.report(execution);
        ScenarioImpactReport invariantReport = ScenarioImpactReport.evaluate(execution, invariants.findings());
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve("execution-report.json").toFile(), execution);
        List<ArtifactReference> artifacts = new ArrayList<>(List.of(
                new ArtifactReference("PACKAGE_MANIFEST", null, null, "UNAVAILABLE", "CONTROLLED_HARNESS_HAS_NO_PERSISTED_FAULT_SCENARIO"),
                ArtifactReference.file("CONTROLLED_SOURCE_CONTRACT", output.resolve("source-contract.json")),
                ArtifactReference.file("EXECUTION_REPORT", output.resolve("execution-report.json"))));
        String buildManifest = System.getProperty("saga.read.experiment.buildManifest");
        artifacts.add(ArtifactReference.file("BUILD_SOURCE_MANIFEST", buildManifest == null ? null : Path.of(buildManifest)));
        long assessmentStart = System.nanoTime();
        SagaReadExposureReport diagnosticReport = diagnostic == null ? null : diagnostic.report(execution, artifacts);
        long diagnosticAssessmentDurationNanos = diagnostic == null ? 0 : System.nanoTime() - assessmentStart;

        // Exact union roots include the collector itself and the assessed report. No JSON-derived surrogate is used.
        Map<String, Object> memory = memory(diagnostic, diagnosticReport);
        verifyProof(execution, impactReport, invariantReport, diagnosticReport);
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve("execution-report.impact-v2.json").toFile(), impactReport);
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve("execution-report.impact.json").toFile(), invariantReport);
        long sidecarBytes = 0;
        if (diagnosticReport != null) {
            Path sidecar = output.resolve("execution-report.saga-read-exposure.json");
            JSON.writerWithDefaultPrettyPrinter().writeValue(sidecar.toFile(), diagnosticReport);
            sidecarBytes = Files.size(sidecar);
        }
        Map<String, Object> result = map("schemaVersion", "microservices-simulator.saga-read-exposure-experiment.v1",
                "caseId", caseId, "serialized", serialized, "diagnosticEnabled", enabled, "executionAttemptId", attempt,
                "fixtureClock", map("method", "Mockito static DateHandler.now; CALLS_REAL_METHODS for other methods",
                        "value", FIXED_CLOCK.toString(), "scope", "main-thread synchronous setup/actions/callbacks",
                        "costBoundary", "clock interception is enabled identically in both diagnostic modes"),
                "proofSurface", "tournament-outer".equals(caseId) ? "CONTROLLED_WORKFLOW_ON_SETUP_RESULT"
                        : "CONTROLLED_WORKFLOW_WITH_ACTUAL_CREATED_ID",
                "actionDurationNanos", actionDurationNanos, "startupDurationNanos", startupDuration,
                "finishDurationNanos", finishDurationNanos, "diagnosticAssessmentDurationNanos", diagnosticAssessmentDurationNanos,
                "setupDurationNanos", setupDurationNanos, "totalDurationNanos", System.nanoTime() - totalStart,
                "timingDefinition", "Sum of real action dispatch/commit/recovery intervals; excludes setup, probes, assessment, JOL, serialization and startup",
                "checks", checks, "workflowReads", workflowReads, "aggregateProofs", aggregateProofs,
                "pendingEventCount", eventService.eventCountForReplay(),
                "horizon", "REPLAY_MODE_RETAIN_PENDING_NO_DELIVERY",
                "impactV1", map("evaluationStatus", invariantReport.evaluationStatus(),
                        "invariantViolationCount", invariantReport.invariantViolationCount()),
                "impactV2", map("assessmentStatus", impactReport.assessmentStatus(), "collectionStatus", impactReport.collectionStatus(),
                        "completeScore", impactReport.completeScore(), "observedAffectedObjectCount", impactReport.observedAffectedObjectCount()),
                "diagnostic", diagnosticSummary(diagnosticReport, sidecarBytes), "memory", memory,
                "limitations", List.of("Positive B input binding occurs in this controlled harness only.",
                        "Pending events are retained and never delivered during this horizon.",
                        "Only exact outer Quiz/Tournament lookup contracts are covered; nested references are excluded.",
                        "The fixed synchronous clock fixture is experimental and equal in both modes.",
                        "JOL measures an inclusive reachable graph, not exclusive retained heap or peak allocation."));
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve("experiment.json").toFile(), result);
        require(runtimeFailure == null && checks.stream().allMatch(value -> Boolean.TRUE.equals(value.get("passed"))),
                "semantic checks failed; retain this run's artifacts");
    }

    private void execute(Planned item, int position) {
        Actor actor;
        // A harness-only input binding/startup is outside measured calls, as ordinary setup is.
        try (var ignored = ReadObservationContext.exclude("SETUP")) {
            actor = actors.computeIfAbsent(item.actor(), unused -> newReader());
        }
        String actionId = actionId(item);
        ImpactEvidence.Writer author = new ImpactEvidence.Writer("SAGA", attempt, workload, item.actor(), actionId,
                item.recovery() ? "RECOVERY" : "FORWARD", actor.functionality.getClass().getName(), item.step(), null);
        if (item.recovery()) {
            recover(actor, item, position, author);
            return;
        }
        var boundary = new FaultVectorBoundaryContext(attempt, workload, item.actor(), occurrenceId(item),
                position, actor.functionality.getClass().getName(), actor.functionality.getClass().getSimpleName(), item.step(), item.fault() ? 1 : 0);
        WorkflowStepExecutionResult result;
        WorkflowFinalizationResult commit = null;
        long start = System.nanoTime();
        try (var writer = ImpactWriterContext.enter(author);
             var provider = FaultVectorProviderHolder.install(new InMemoryFaultVectorProvider(item.fault()
                     ? Map.of(position, FaultVectorFault.from(boundary)) : Map.of()));
             var faultScope = FaultVectorProviderHolder.enterBoundary(boundary)) {
            result = actor.functionality.executeStepForExecutorControlled(item.step(), actor.uow);
            if (result.completed() && item.commit()) commit = actor.functionality.finalizeForExecutor(actor.uow);
        } finally {
            actionDurationNanos += System.nanoTime() - start;
        }
        Throwable failure = result.failure();
        String status = result.completed() ? "COMPLETED" : item.fault() ? "ASSIGNED_FAULT" : "FAILED";
        String commitOutcome = commit == null ? "NOT_RUN" : commit.committed() ? "SUCCEEDED" : "FAILED";
        if (commit != null && !commit.committed()) { failure = commit.failure(); status = "COMMIT_FAILED"; }
        actions.add(outcome(item, position, status, result.completed() ? "SUCCEEDED" : item.fault() ? "NOT_RUN" : "FAILED",
                commitOutcome, List.of(), failure));
        if (commit != null && commit.committed()) {
            actor.state = "COMMITTED";
            lifecycle(actor, item, "AUTOMATIC_COMMIT", "SUCCEEDED");
        } else if (!result.completed()) {
            actor.state = "ABORTED";
            lifecycle(actor, item, "ABORTED", status);
        }
        if (item.fault()) {
            require(failure instanceof FaultVectorInjectedFaultException,
                    "creating tail must fail at the real assigned boundary: " + failure);
            require(actor.uow.getExecutedSteps().equals(CREATE_PREFIX), "assigned fault unexpectedly executed the creation tail");
            List<String> pending = actor.functionality.recoveryCheckpointsForExecutor(actor.uow).stream()
                    .map(WorkflowRecoveryCheckpoint::sourceStepName).toList();
            require(pending.equals(CREATE_RECOVERY), "actual recovery checkpoints differ from the declared controlled source: " + pending);
        } else if (item.missing()) {
            require(failure instanceof SimulatorDomainException missing
                            && SimulatorErrorMessage.AGGREGATE_NOT_FOUND.equals(missing.getErrorMessage())
                            && String.format(SimulatorErrorMessage.AGGREGATE_NOT_FOUND, quizId).equals(missing.getMessage()),
                    "reader failure must identify exactly the compensated Quiz: " + failure);
            require(actor.functionality.recoveryCheckpointsForExecutor(actor.uow).isEmpty(), "failed read acquired unexpected recovery work");
            actor.state = "COMPENSATED";
            lifecycle(actor, item, "COMPENSATION_COMPLETED", "NO_PENDING_CHECKPOINTS");
        } else {
            require(result.completed() && (commit == null || commit.committed()), "unexpected action failure: " + failure);
        }
        if ("generateQuizStep".equals(item.step()) && result.completed()) {
            QuizDto dto = ((CreateTournamentFunctionalitySagas) actor.functionality).getQuizDto();
            quizId = dto.getAggregateId(); createdQuizVersion = dto.getVersion();
            var persisted = exact("SagaQuiz", quizId, createdQuizVersion);
            require(persisted != null && "ACTIVE".equals(persisted.lifecycleState()) && persisted.frameworkMetadata().predecessorVersion() == null,
                    "producer did not persist a new active Quiz with no predecessor");
            aggregateProofs.put("createdQuiz", revision(persisted));
        }
        if (result.completed() && Set.of("getQuizStep", "findQuizStep", "findTournamentStep").contains(item.step())) {
            recordWorkflowRead(actor, item);
        }
    }

    private void recover(Actor actor, Planned item, int position, ImpactEvidence.Writer author) {
        WorkflowStepRecoveryResult result = null;
        Throwable failure = null;
        String failedKind = null;
        long start = System.nanoTime();
        try (var writer = ImpactWriterContext.enter(author)) {
            result = actor.functionality.recoverStepForExecutor(item.step(), actor.uow);
        } catch (WorkflowStepRecoveryException error) {
            result = error.completedRecovery(); failedKind = error.failedRecoveryKind(); failure = error.getCause();
        } catch (Throwable error) {
            failure = error;
        } finally {
            actionDurationNanos += System.nanoTime() - start;
        }
        List<ScenarioExecutionReport.RecoverySubOutcome> sub = new ArrayList<>();
        if (result != null && result.explicitCompensationExecuted()) sub.add(new ScenarioExecutionReport.RecoverySubOutcome(EXPLICIT, "SUCCEEDED"));
        if (result != null && result.implicitRollbackExecuted()) sub.add(new ScenarioExecutionReport.RecoverySubOutcome(IMPLICIT, "SUCCEEDED"));
        if (failedKind != null) sub.add(new ScenarioExecutionReport.RecoverySubOutcome(failedKind, "FAILED",
                failure == null ? null : failure.getClass().getName(), failure == null ? null : failure.getMessage()));
        actions.add(outcome(item, position, failure == null ? "COMPENSATED" : "COMPENSATION_FAILED",
                "NOT_RUN", "NOT_RUN", sub, failure));
        if (failure != null) actor.state = "COMPENSATION_FAILED";
        if ("generateQuizStep".equals(item.step())) {
            require(result != null && result.explicitCompensationExecuted(), "actual explicit Quiz compensation did not succeed");
            var deleted = latest("SagaQuiz", quizId);
            aggregateProofs.put("deletedQuiz", revision(deleted));
            check("confirmed direct-predecessor deletion", deleted != null && "DELETED".equals(deleted.lifecycleState())
                            && Objects.equals(createdQuizVersion, deleted.frameworkMetadata().predecessorVersion()),
                    map("createdVersion", createdQuizVersion, "deletedRevision", revision(deleted)));
        }
        require(failure == null, "recovery failed; completed explicit suboutcomes remain recorded: " + failure);
        if (actor.functionality.recoveryCheckpointsForExecutor(actor.uow).isEmpty()) {
            actor.state = "COMPENSATED";
            lifecycle(actor, item, "COMPENSATION_COMPLETED", "SUCCEEDED");
        }
    }

    private void recordWorkflowRead(Actor actor, Planned item) {
        boolean tournament = actor.functionality instanceof FindTournamentFunctionalitySagas;
        Integer id; Long version;
        if (tournament) {
            TournamentDto dto = ((FindTournamentFunctionalitySagas) actor.functionality).getTournamentDto();
            id = dto.getAggregateId(); version = dto.getVersion();
            Map<String, Object> reference = probe(() -> {
                SagaTournament persisted = context.getBean(EntityManager.class).createQuery(
                        "select t from SagaTournament t where t.aggregateId=:id and t.version=:version", SagaTournament.class)
                        .setParameter("id", dto.getAggregateId()).setParameter("version", dto.getVersion()).getSingleResult();
                return map("quizAggregateId", persisted.getTournamentQuiz().getQuizAggregateId(),
                        "quizVersion", persisted.getTournamentQuiz().getQuizVersion());
            });
            aggregateProofs.put("tournament", map("aggregateId", id, "version", version,
                    "nestedQuizAggregateId", dto.getQuiz().getAggregateId(), "nestedQuizVersion", dto.getQuiz().getVersion(),
                    "persistedNestedReference", reference,
                    "nestedCoverage", "UNCOVERED_NESTED_REFERENCE_WITHOUT_RETURNED_REVISION"));
            check("persisted nested Quiz revision is not present in the returned nested DTO", dto.getQuiz().getVersion() == null
                            && reference.get("quizVersion") != null && Objects.equals(reference.get("quizAggregateId"), dto.getQuiz().getAggregateId()),
                    map("nestedQuizAggregateId", dto.getQuiz().getAggregateId(), "nestedQuizVersion", dto.getQuiz().getVersion(),
                            "persistedNestedReference", reference));
        } else {
            QuizDto dto = actor.functionality instanceof StartQuizFunctionalitySagas start
                    ? start.getQuizDto() : ((FindQuizFunctionalitySagas) actor.functionality).getQuizDto();
            id = dto.getAggregateId(); version = dto.getVersion();
            aggregateProofs.put("retainedQuiz", map("aggregateId", id, "version", version));
        }
        String type = tournament ? "SagaTournament" : "SagaQuiz";
        String runtimeType = tournament ? SagaTournament.class.getName() : SagaQuiz.class.getName();
        var persisted = exact(type, id, version);
        check("workflow-retained DTO matches exact persisted revision", persisted != null
                        && Objects.equals(id, persisted.identity().aggregateId()) && Objects.equals(version, persisted.version())
                        && Objects.equals(runtimeType, persisted.runtimeType()), map("actor", item.actor(), "step", item.step(), "version", version));
        workflowReads.add(map("actor", item.actor(), "step", item.step(), "aggregateType", type,
                "runtimeType", runtimeType, "aggregateId", id, "version", version, "persistedRevision", revision(persisted)));
    }

    private void verifyProof(ScenarioExecutionReport execution, ImpactV2EvidenceReport impact,
                             ScenarioImpactReport invariants, SagaReadExposureReport diagnostic) {
        if (runtimeFailure != null) return;
        check("all declared actions executed in order", actions.size() == plan.size()
                && "EXACT".equals(execution.scheduleConformance()), map("planned", plan.size(), "actual", actions.size()));
        check("ImpactV1 collector measured the controlled attempt", "EVALUATED".equals(invariants.evaluationStatus()),
                map("invariantViolationCount", invariants.invariantViolationCount()));
        check("existing write collection remained enabled", !"UNAVAILABLE".equals(impact.collectionStatus()),
                map("collectionStatus", impact.collectionStatus(), "committedWriteCount", impact.committedWrites().size()));
        Actor reader = actors.get("B");
        if ("reader-only".equals(caseId)) {
            long readerWrites = impact.committedWrites().stream().filter(write -> write.writer() != null
                    && "B".equals(write.writer().sagaInstanceId())).count();
            check("reader-only B commits before A compensation without writes", "COMMITTED".equals(reader.state) && readerWrites == 0
                            && actionPosition("B", "findQuizStep", false) < actionPosition("A", "generateQuizStep", true),
                    map("readerFinalState", reader.state, "readerWriteCount", readerWrites));
        }
        if ("early-compensation".equals(caseId)) {
            check("compensation precedes failing B lookup", workflowReads.isEmpty()
                            && actionPosition("A", "generateQuizStep", true) < actionPosition("B", "getQuizStep", false),
                    map("readerFinalState", reader.state));
            Set<Integer> answerIds = probe(() -> context.getBean(QuizAnswerRepository.class).findAllAggregateIds());
            check("early failing read created no QuizAnswer", answerIds.isEmpty(), map("answerIds", new TreeSet<>(answerIds)));
        }
        if ("producer-success".equals(caseId)) {
            check("B received A's creation before A completed successfully", "COMMITTED".equals(actors.get("A").state)
                            && actionPosition("A", "generateQuizStep", false) < actionPosition("B", "getQuizStep", false)
                            && actionPosition("B", "getQuizStep", false) < actionPosition("A", "createTournamentStep", false)
                            && "ACTIVE".equals(latest("SagaQuiz", quizId).lifecycleState()), map("producerFinalState", actors.get("A").state));
        }
        if (reader != null && reader.functionality instanceof StartQuizFunctionalitySagas start && start.getQuizAnswerDto() != null) {
            Integer answerId = start.getQuizAnswerDto().getAggregateId();
            var answer = latest("SagaQuizAnswer", answerId);
            Map<String, Object> answerReference = probe(() -> {
                var persisted = context.getBean(QuizAnswerRepository.class).findLastAggregateVersion(answerId).orElseThrow();
                return map("lifecycleState", persisted.getState().name(), "quizAggregateId", persisted.getQuiz().getQuizAggregateId(),
                        "quizVersion", persisted.getQuiz().getQuizVersion());
            });
            aggregateProofs.put("answer", map("revision", revision(answer), "persistedReference", answerReference));
            check("StartQuiz completed and kept the actually delivered Quiz revision", "COMMITTED".equals(reader.state)
                    && Objects.equals(start.getQuizDto().getVersion(), createdQuizVersion)
                    && "ACTIVE".equals(answerReference.get("lifecycleState"))
                    && Objects.equals(quizId, answerReference.get("quizAggregateId"))
                    && Objects.equals(createdQuizVersion, answerReference.get("quizVersion")),
                    map("answerId", answerId, "retainedVersion", start.getQuizDto().getVersion(), "persistedReference", answerReference));
            if ("split-start".equals(caseId)) check("split StartQuiz persisted an active answer after its Quiz was deleted",
                    "DELETED".equals(latest("SagaQuiz", quizId).lifecycleState())
                            && actionPosition("A", "generateQuizStep", true) < actionPosition("B", "startQuizStep", false),
                    map("answerId", answerId, "quizId", quizId));
        }
        if (diagnostic == null) {
            check("disabled diagnostic creates no sidecar", !Files.exists(output.resolve("execution-report.saga-read-exposure.json")), Map.of());
            return;
        }
        for (Map<String, Object> retained : workflowReads) {
            List<Call> actual = diagnostic.calls().stream().filter(call -> call.observation().outcome() == ReadResponseEvidence.Outcome.DELIVERED
                    && call.observation().reader().sagaInstanceId().equals(retained.get("actor"))
                    && call.observation().reader().stepName().equals(retained.get("step"))).toList();
            check("generic final-return fact agrees with workflow and persistence", actual.size() == 1
                            && Objects.equals(actual.getFirst().observation().identity().aggregateId(), retained.get("aggregateId"))
                            && Objects.equals(actual.getFirst().observation().version(), retained.get("version"))
                            && actual.getFirst().observation().serialized() == serialized,
                    map("actor", retained.get("actor"), "step", retained.get("step"), "matchingDeliveredFacts", actual.size()));
        }
        boolean positive = Set.of("split-start", "reader-only").contains(caseId);
        check("generic assessor returns the declared case predicate", Objects.equals(diagnostic.observedExposureCount(), positive ? 1 : 0),
                map("expectedCount", positive ? 1 : 0, "observedCount", diagnostic.observedExposureCount(), "coverage", diagnostic.collectionCoverage()));
        if (positive && diagnostic.findings().size() == 1) {
            Finding finding = diagnostic.findings().getFirst();
            check("finding joins exact A creation, B delivery and creating-step compensation", "A".equals(finding.producerSagaId())
                    && "B".equals(finding.readerSagaId()) && Objects.equals(createdQuizVersion, finding.createdVersion())
                    && checkpointId("A", "generateQuizStep").equals(finding.checkpointId()), map("finding", finding));
        }
        if ("early-compensation".equals(caseId)) check("failed B call was never a delivery",
                diagnostic.calls().stream().noneMatch(call -> call.observation().outcome() == ReadResponseEvidence.Outcome.DELIVERED)
                        && diagnostic.calls().stream().anyMatch(call -> call.observation().outcome() == ReadResponseEvidence.Outcome.FAILED
                        && "B".equals(call.observation().reader().sagaInstanceId())), Map.of());
        if ("tournament-outer".equals(caseId)) check("Tournament call creates only an outer Tournament fact",
                diagnostic.calls().stream().filter(call -> call.observation().outcome() == ReadResponseEvidence.Outcome.DELIVERED)
                        .allMatch(call -> "SagaTournament".equals(call.observation().identity().aggregateType()))
                        && diagnostic.excludedPaths().contains("NESTED_REFERENCES"), Map.of());
    }

    private Map<String, Object> memory(SagaReadExposureCollector collector, SagaReadExposureReport report) throws Exception {
        if (collector == null) return map("status", "DISABLED", "method", "JOL_0.17_UNION_ROOTS", "graphObjectCount", 0,
                "graphBytes", 0, "semantics", "No diagnostic collector or report is allocated; not a JVM heap measurement",
                "jvmArguments", ManagementFactory.getRuntimeMXBean().getInputArguments());
        GraphLayout graph = GraphLayout.parseInstance(collector, report);
        Files.writeString(output.resolve("memory-footprint.txt"), graph.toFootprint());
        return map("status", "MEASURED", "method", "JOL_0.17_UNION_ROOTS", "graphObjectCount", graph.totalCount(),
                "graphBytes", graph.totalSize(), "roots", List.of(collector.getClass().getName(), report.getClass().getName()),
                "semantics", "Inclusive reachable graph at completed horizon before diagnostic JSON; shared references included; neither exclusive retained heap nor peak allocation",
                "jvmArguments", ManagementFactory.getRuntimeMXBean().getInputArguments());
    }

    private Map<String, Object> diagnosticSummary(SagaReadExposureReport report, long sidecarBytes) {
        if (report == null) return null;
        Map<String, Long> counts = new TreeMap<>();
        for (var value : ReadResponseEvidence.Outcome.values()) counts.put(value.name(), 0L);
        report.calls().forEach(call -> counts.merge(call.observation().outcome().name(), 1L, Long::sum));
        return map("coverage", report.collectionCoverage(), "count", report.observedExposureCount(), "callOutcomeCounts", counts,
                "copiedWriteCount", report.committedWrites().size(), "retainedBaselineCount", report.baseline().size(),
                "retainedActionCount", report.actions().size(), "findingCount", report.findings().size(), "gapCount", report.gaps().size(),
                "sidecarBytes", sidecarBytes, "retentionLimit", "NONE; facts are retained for this attempt");
    }

    private ScenarioExecutionReport executionReport() {
        boolean allCommitted = actors.values().stream().allMatch(actor -> "COMMITTED".equals(actor.state));
        boolean someCommitted = actors.values().stream().anyMatch(actor -> "COMMITTED".equals(actor.state));
        String terminal = runtimeFailure != null ? "UNEXPECTED_EXECUTION_FAILURE" : allCommitted ? "SUCCESS"
                : someCommitted ? "PARTIAL_COMPENSATED" : "COMPENSATED";
        List<ScenarioExecutionReport.PlannedAction> planned = new ArrayList<>();
        for (int position = 0; position < plan.size(); position++) {
            Planned item = plan.get(position);
            planned.add(new ScenarioExecutionReport.PlannedAction(actionId(item), item.recovery() ? "COMPENSATION" : "FORWARD",
                    item.actor(), item.recovery() ? null : "slot:" + occurrenceId(item), item.recovery() ? checkpointId(item.actor(), item.step()) : null,
                    null, occurrenceId(item), item.step() + "#0", item.step(), item.recovery() ? evidenceClass(item.step()) : null,
                    null, null, null, null, null, position));
        }
        var participants = actors.entrySet().stream().map(entry -> new ScenarioExecutionReport.Participant(entry.getKey(),
                entry.getValue().functionality.getClass().getName(), "controlled-input:" + entry.getKey(), "MATERIALIZED", "STARTED",
                entry.getValue().state, List.of(), List.of())).toList();
        return new ScenarioExecutionReport(null, attempt, terminal, null, workload, scenario, "CONTROLLED_APPLICATION_WORKFLOW",
                null, "IN_MEMORY_FAULT_VECTOR", runtimeFailure == null ? "EXACT" : "INCOMPLETE", null, null, null,
                runtimeFailure == null || actions.isEmpty() ? null : actions.getLast().actionId(),
                runtimeFailure == null ? null : "CONTROLLED_CASE_FAILED",
                new ScenarioExecutionReport.RuntimeMetadata("quizzes", "quizzes", QuizzesSimulator.class.getName(), "test,sagas,local",
                        "test-sagas", null, scenario, "CONTROLLED_APPLICATION_HARNESS", false),
                null, null, List.of(), planned, actions, lifecycle, participants, List.of());
    }

    private ScenarioExecutionReport.ActionOutcome outcome(Planned item, int position, String status, String body, String commit,
                                                          List<ScenarioExecutionReport.RecoverySubOutcome> sub, Throwable failure) {
        return new ScenarioExecutionReport.ActionOutcome(actionId(item), item.recovery() ? "COMPENSATION" : "FORWARD", item.actor(),
                item.recovery() ? null : "slot:" + occurrenceId(item), item.recovery() ? checkpointId(item.actor(), item.step()) : null,
                null, occurrenceId(item), item.step() + "#0", item.step(), item.recovery() ? evidenceClass(item.step()) : null,
                item.recovery() ? recoveryOccurrenceId(item.actor(), item.step()) : occurrenceId(item), position, actions.size(),
                status, body, commit, item.fault() ? "ASSIGNED" : failure == null ? null : "UNASSIGNED_RUNTIME", null, sub,
                failure == null ? null : failure.getClass().getName(), failure == null ? null : failure.getMessage());
    }

    private void lifecycle(Actor actor, Planned item, String type, String outcome) {
        lifecycle.add(new ScenarioExecutionReport.LifecycleEvent(lifecycle.size(), item.actor(), type, actionId(item), outcome, null, null));
    }

    private int actionPosition(String actor, String step, boolean recovery) {
        return actions.stream().filter(action -> actor.equals(action.sagaInstanceId()) && step.equals(action.runtimeStepName())
                        && (recovery ? "COMPENSATION" : "FORWARD").equals(action.kind()))
                .mapToInt(ScenarioExecutionReport.ActionOutcome::actualPosition).findFirst().orElse(Integer.MAX_VALUE);
    }

    private ScenarioRuntimeContext runtimeContext() {
        return new ScenarioRuntimeContext() {
            public Object bean(Class<?> type) { return context.getBean(type); }
            public <T> List<T> beans(Class<T> type) { return List.copyOf(context.getBeansOfType(type).values()); }
        };
    }

    private ImpactEvidence.AggregateSnapshot exact(String type, Integer id, Long version) {
        try (var ignored = ReadObservationContext.exclude("PROBE")) {
            var result = stateObserver.snapshotVersion(new ImpactEvidence.AggregateIdentity(type, id), version, "PROBE");
            require(result.gaps().isEmpty(), "persistent exact-version proof contains gaps: " + result.gaps());
            return result.snapshot();
        }
    }

    private ImpactEvidence.AggregateSnapshot latest(String type, Integer id) {
        try (var ignored = ReadObservationContext.exclude("PROBE")) {
            var snapshot = stateObserver.snapshotAll();
            return snapshot.aggregates().stream().filter(value -> type.equals(value.identity().aggregateType())
                            && Objects.equals(id, value.identity().aggregateId())).findFirst().orElse(null);
        }
    }

    private <T> T probe(Supplier<T> operation) {
        try (var ignored = ReadObservationContext.exclude("PROBE")) {
            return new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).execute(status -> {
                context.getBean(EntityManager.class).clear();
                return operation.get();
            });
        }
    }

    private static Map<String, Object> revision(ImpactEvidence.AggregateSnapshot value) {
        if (value == null) return null;
        return map("aggregateType", value.identity().aggregateType(), "aggregateId", value.identity().aggregateId(),
                "runtimeType", value.runtimeType(), "version", value.version(), "lifecycleState", value.lifecycleState(),
                "predecessorIdentity", value.frameworkMetadata() == null ? null : value.frameworkMetadata().predecessorIdentity(),
                "predecessorVersion", value.frameworkMetadata() == null ? null : value.frameworkMetadata().predecessorVersion());
    }

    private Actor newCreate() {
        var uow = uowService.createUnitOfWork(CreateTournamentFunctionalitySagas.class.getSimpleName());
        return new Actor(new CreateTournamentFunctionalitySagas(uowService, setup.creatorId(), setup.courseId(),
                List.of(setup.topic1Id(), setup.topic2Id()), tournamentInput(), uow, gateway), uow);
    }

    private Actor newReader() {
        if ("tournament-outer".equals(caseId)) {
            var uow = uowService.createUnitOfWork(FindTournamentFunctionalitySagas.class.getSimpleName());
            return new Actor(new FindTournamentFunctionalitySagas(uowService, setup.tournamentId(), uow, gateway), uow);
        }
        require(quizId != null, "B may only start after A returns the actual newly created Quiz ID");
        if ("reader-only".equals(caseId)) {
            var uow = uowService.createUnitOfWork(FindQuizFunctionalitySagas.class.getSimpleName());
            return new Actor(new FindQuizFunctionalitySagas(uowService, quizId, uow, gateway), uow);
        }
        var uow = uowService.createUnitOfWork(StartQuizFunctionalitySagas.class.getSimpleName());
        return new Actor(new StartQuizFunctionalitySagas(uowService, quizId, setup.courseId(), setup.studentId(), uow, gateway), uow);
    }

    private Setup setupApplication(boolean withTournament) {
        var executionFns = context.getBean(ExecutionFunctionalities.class);
        var userFns = context.getBean(UserFunctionalities.class);
        var topicFns = context.getBean(TopicFunctionalities.class);
        var questionFns = context.getBean(QuestionFunctionalities.class);
        CourseExecutionDto course = new CourseExecutionDto();
        course.setName("BLCM"); course.setType("TECNICO"); course.setAcronym("TESTBLCM");
        course.setAcademicTerm("2029/2030"); course.setEndDate(DateHandler.toISOString(FIXED_CLOCK.plusHours(2)));
        course = executionFns.createCourseExecution(course);
        UserDto creator = user("Fixture Creator", "fixture-creator", userFns);
        UserDto student = user("Fixture Reader", "fixture-reader", userFns);
        executionFns.addStudent(course.getAggregateId(), creator.getAggregateId());
        executionFns.addStudent(course.getAggregateId(), student.getAggregateId());
        TopicDto first = topic("Fixture Topic One", course.getCourseAggregateId(), topicFns);
        TopicDto second = topic("Fixture Topic Two", course.getCourseAggregateId(), topicFns);
        question("Question One", course.getCourseAggregateId(), first, questionFns);
        question("Question Two", course.getCourseAggregateId(), second, questionFns);
        Integer tournament = null;
        if (withTournament) tournament = context.getBean(TournamentFunctionalities.class).createTournament(creator.getAggregateId(),
                course.getAggregateId(), List.of(first.getAggregateId(), second.getAggregateId()), tournamentInput()).getAggregateId();
        return new Setup(course.getAggregateId(), creator.getAggregateId(), student.getAggregateId(),
                first.getAggregateId(), second.getAggregateId(), tournament);
    }

    private static UserDto user(String name, String username, UserFunctionalities functions) {
        UserDto input = new UserDto(); input.setName(name); input.setUsername(username); input.setRole("STUDENT");
        UserDto result = functions.createUser(input); functions.activateUser(result.getAggregateId()); return result;
    }

    private static TopicDto topic(String name, int courseId, TopicFunctionalities functions) {
        TopicDto input = new TopicDto(); input.setName(name); return functions.createTopic(courseId, input);
    }

    private static void question(String title, int courseId, TopicDto topic, QuestionFunctionalities functions) {
        QuestionDto input = new QuestionDto(); input.setTitle(title); input.setContent(title + " content"); input.setTopicDto(Set.of(topic));
        OptionDto first = new OptionDto(); first.setSequence(1); first.setCorrect(true); first.setContent("Correct option");
        OptionDto second = new OptionDto(); second.setSequence(2); second.setCorrect(false); second.setContent("Other option");
        input.setOptionDtos(List.of(first, second)); functions.createQuestion(courseId, input);
    }

    private static TournamentDto tournamentInput() {
        TournamentDto input = new TournamentDto(); input.setStartTime(DateHandler.toISOString(FIXED_CLOCK.plusMinutes(5)));
        input.setEndTime(DateHandler.toISOString(FIXED_CLOCK.plusHours(1))); input.setNumberOfQuestions(2); return input;
    }

    private static List<Planned> plan(String caseId) {
        List<Planned> result = new ArrayList<>();
        if ("tournament-outer".equals(caseId)) return List.of(new Planned("B", "findTournamentStep", false, false, true, false));
        CREATE_PREFIX.forEach(step -> result.add(new Planned("A", step, false, false, false, false)));
        boolean success = "producer-success".equals(caseId);
        if (!success) result.add(new Planned("A", "createTournamentStep", false, true, false, false));
        if ("early-compensation".equals(caseId)) recoveryPlan(result);
        if ("reader-only".equals(caseId)) result.add(new Planned("B", "findQuizStep", false, false, true, false));
        else result.add(new Planned("B", "getQuizStep", false, false, false, "early-compensation".equals(caseId)));
        if (success) result.add(new Planned("A", "createTournamentStep", false, false, true, false));
        if (Set.of("split-start", "reader-only").contains(caseId)) recoveryPlan(result);
        if (Set.of("split-start", "producer-success").contains(caseId)) {
            result.add(new Planned("B", "getUserStep", false, false, false, false));
            result.add(new Planned("B", "startQuizStep", false, false, true, false));
        }
        return List.copyOf(result);
    }

    private static void recoveryPlan(List<Planned> plan) {
        CREATE_RECOVERY.forEach(step -> plan.add(new Planned("A", step, true, false, false, false)));
    }

    private static SourceContract sourceContract(List<Planned> plan) {
        List<Occurrence> occurrences = new ArrayList<>();
        List<Checkpoint> checkpoints = new ArrayList<>();
        for (int position = 0; position < plan.size(); position++) {
            Planned item = plan.get(position);
            if (!item.recovery()) occurrences.add(new Occurrence(occurrenceId(item), item.actor(), item.step() + "#0", item.step(), position));
            else checkpoints.add(new Checkpoint(checkpointId(item.actor(), item.step()), item.actor(), occurrenceId(item),
                    item.step() + "#0", item.step(), recoveryOccurrenceId(item.actor(), item.step()), evidenceClass(item.step())));
        }
        return new SourceContract(occurrences, checkpoints);
    }

    private void check(String name, boolean passed, Map<String, Object> details) {
        checks.add(map("name", name, "passed", passed, "details", details));
    }
    private static boolean bool(String value) { require(Set.of("true", "false").contains(value), "expected true or false"); return Boolean.parseBoolean(value); }
    private static String occurrenceId(Planned item) { return item.actor() + ":" + item.step() + "#0"; }
    private static String checkpointId(String actor, String step) { return "checkpoint:" + actor + ":" + step + "#0"; }
    private static String recoveryOccurrenceId(String actor, String step) { return "recovery:" + actor + ":" + step + "#0"; }
    private static String actionId(Planned item) { return (item.recovery() ? "compensate:" : "forward:") + occurrenceId(item); }
    private static String evidenceClass(String step) { return "generateQuizStep".equals(step) ? EXPLICIT : IMPLICIT; }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static Map<String, Object> map(Object... pairs) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) result.put((String) pairs[index], pairs[index + 1]);
        return result;
    }
    private record Setup(int courseId, int creatorId, int studentId, int topic1Id, int topic2Id, Integer tournamentId) { }
    private record Planned(String actor, String step, boolean recovery, boolean fault, boolean commit, boolean missing) { }
    private static final class Actor {
        final WorkflowFunctionality functionality;
        final SagaUnitOfWork uow;
        String state = "STARTED";
        Actor(WorkflowFunctionality functionality, SagaUnitOfWork uow) { this.functionality = functionality; this.uow = uow; }
    }
}
