package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.experiments;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.*;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorDomainException;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.*;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventReplayCoordinator;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuestionAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.functionalities.QuizAnswerFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.StartQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.CreateTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletionException;

/** Disposable, application-side research observer for the bounded impact experiment. */
public final class ImpactExperiment {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> CREATE_PREFIX = List.of(
            "getCourseExecutionStep", "getCreatorStep", "getTopicsStep",
            "findQuestionsByTopicIdsStep", "getCourseExecutionById", "generateQuizStep");

    private final ConfigurableApplicationContext context;
    private final TransactionTemplate tx;
    private final EntityManager entityManager;
    private final EventService eventService;
    private final SagaUnitOfWorkService uowService;
    private final LocalCommandGateway gateway;
    private final RecordingRecorder recorder = new RecordingRecorder();
    private final List<Map<String, Object>> timeline = new ArrayList<>();
    private final List<Map<String, Object>> checks = new ArrayList<>();
    private final Map<String, String> actorStatuses = new LinkedHashMap<>();
    private final Map<String, String> actorFunctionalities = new LinkedHashMap<>();
    private final LinkedHashSet<Integer> focusIds = new LinkedHashSet<>();
    private Setup setup;
    private int setupPendingEventsCleared;
    private int initialPendingEvents;
    private Map<String, Object> initialSnapshot;

    private ImpactExperiment(ConfigurableApplicationContext context) {
        this.context = context;
        this.tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        this.entityManager = context.getBean(EntityManager.class);
        this.eventService = context.getBean(EventService.class);
        this.uowService = context.getBean(SagaUnitOfWorkService.class);
        this.gateway = context.getBean(LocalCommandGateway.class);
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 2 || (args.length == 3 && args[0].equals("--behavior-probe")),
                "usage: ImpactExperiment CASE_ID OUTPUT_PATH | --behavior-probe PROBE_ID OUTPUT_PATH");
        System.setProperty("spring.profiles.active", "test,sagas,local");
        System.setProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY, "true");
        try (var replay = EventReplayCoordinator.activate();
             var context = SpringApplication.run(QuizzesSimulator.class,
                     "--verifiers.application.enabled=false", "--server.port=0")) {
            var experiment = new ImpactExperiment(context);
            boolean behaviorProbe = args.length == 3;
            String id = behaviorProbe ? args[1] : args[0];
            String output = behaviorProbe ? args[2] : args[1];
            Map<String, Object> report = behaviorProbe ? experiment.runBehaviorProbe(id) : experiment.run(id);
            JSON.writerWithDefaultPrettyPrinter().writeValue(Path.of(output).toFile(), report);
            System.out.println((behaviorProbe ? "IMPACT_BEHAVIOR_PROBE_PASS " : "IMPACT_EXPERIMENT_PASS ") + id);
        } finally {
            DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
            FaultVectorProviderHolder.clear();
        }
    }

    private Map<String, Object> run(String caseId) {
        boolean needsTournament = caseId.equals("remove-fault-recovery-alone")
                || caseId.equals("healthy-tournament-control");
        setup = setupApplication(needsTournament);
        if (setup.tournamentId != null) focusIds.add(setup.tournamentId);
        setupPendingEventsCleared = pendingEvents();
        eventService.clearEventsForReplay();
        require(pendingEvents() == 0, "setup pending-event baseline was not cleared");
        initialPendingEvents = pendingEvents();
        initialSnapshot = snapshot();
        DynamicEvidenceRecorderHolder.setRecorder(recorder);
        check("empty measured pending-event baseline", initialPendingEvents == 0,
                Map.of("setupPendingEventsCleared", setupPendingEventsCleared, "initialPendingEventCount", initialPendingEvents));

        String description;
        switch (caseId) {
            case "create-early-compensation" -> description = createEarlyCompensation();
            case "create-split-start" -> description = createSplitStart();
            case "create-success-overlap-control" -> description = createSuccessOverlapControl();
            case "remove-fault-recovery-alone" -> description = removeFaultRecoveryAlone();
            case "healthy-tournament-control" -> description = healthyTournamentControl();
            default -> throw new IllegalArgumentException("unknown case " + caseId);
        }
        DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
        Map<String, Object> finalSnapshot = snapshot();
        int finalPending = pendingEvents();
        check("pending-event horizon retained", finalPending >= initialPendingEvents,
                Map.of("initial", initialPendingEvents, "final", finalPending));

        List<Map<String, Object>> actors = actorStatuses.entrySet().stream().map(e -> {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", e.getKey());
            a.put("functionality", actorFunctionalities.get(e.getKey()));
            a.put("terminalStatus", e.getValue());
            return a;
        }).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemaVersion", "microservices-simulator.impact-experiment.v1");
        result.put("caseId", caseId);
        result.put("description", description);
        result.put("actors", actors);
        result.put("horizon", Map.of(
                "policy", "REPLAY_MODE_RETAIN_PENDING_NO_DELIVERY",
                "setupPendingEventsCleared", setupPendingEventsCleared,
                "initialPendingEventCount", initialPendingEvents,
                "retainedPendingEventCount", finalPending,
                "explicitLimit", "No pending event is delivered; observation ends after the listed controlled actions."));
        result.put("initialSnapshot", initialSnapshot);
        result.put("timeline", timeline);
        result.put("finalSnapshot", finalSnapshot);
        result.put("pendingEventCount", finalPending);
        result.put("checks", checks);
        result.put("limitations", List.of(
                "AGGREGATE_ACCESSED identifies reader/writer operations but does not identify the exact aggregate version read.",
                "Typed references are observed relationship facts; this report does not assert a general required-live-target policy.",
                "Pending application events are retained under an explicit no-delivery horizon; no blanket drain is performed.",
                "Setup is excluded from measured events and snapshot reads run with dynamic recording disabled."));
        require(checks.stream().allMatch(c -> Boolean.TRUE.equals(c.get("passed"))), "case checks failed");
        return result;
    }

    private Map<String, Object> runBehaviorProbe(String probeId) {
        String baseCase = switch (probeId) {
            case "tournament-read-affected", "tournament-quiz-read-affected" -> "remove-fault-recovery-alone";
            case "tournament-read-healthy", "tournament-quiz-read-healthy" -> "healthy-tournament-control";
            case "answer-question-affected" -> "create-split-start";
            case "answer-question-healthy" -> "create-success-overlap-control";
            default -> throw new IllegalArgumentException("unknown behavior probe " + probeId);
        };
        Map<String, Object> original = run(baseCase);
        Map<String, Object> before = snapshot();
        int pendingBefore = pendingEvents();
        List<Map<String, Object>> locksBefore = lockEvidence();
        require(before.equals(original.get("finalSnapshot")), "probe did not begin from the recorded original final snapshot");
        require(pendingBefore == ((Number) original.get("pendingEventCount")).intValue(),
                "probe pending-event baseline differs from original horizon");

        recorder.clear();
        DynamicEvidenceRecorderHolder.setRecorder(recorder);
        Map<String, Object> arguments = new LinkedHashMap<>();
        Map<String, Object> selection = new LinkedHashMap<>();
        Map<String, Object> outcome = new LinkedHashMap<>();
        Map<String, Object> answerBefore = answerObservation();
        try {
            Object returned;
            if (probeId.startsWith("tournament-read-")) {
                returned = probeFindTournament(arguments);
            } else if (probeId.startsWith("tournament-quiz-read-")) {
                returned = probeTournamentThenQuiz(arguments);
            } else {
                returned = probeAnswerQuestion(arguments, selection);
            }
            outcome.put("status", "SUCCESS");
            outcome.put("returned", returned);
            outcome.put("exceptionChain", List.of());
        } catch (Throwable failure) {
            outcome.put("status", "ERROR");
            outcome.put("returned", null);
            outcome.put("exceptionChain", exceptionChain(failure));
        }
        List<Map<String, Object>> events = recorder.drain();
        outcome.put("errorStep", errorStep(events));
        DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());

        Map<String, Object> after = snapshot();
        int pendingAfter = pendingEvents();
        List<Map<String, Object>> locksAfter = lockEvidence();
        Map<String, Object> answerAfter = answerObservation();
        Map<String, Object> mutation = new LinkedHashMap<>();
        mutation.put("completeObservedSliceChanged", !before.equals(after));
        mutation.put("answerBefore", answerBefore);
        mutation.put("answerAfter", answerAfter);
        mutation.put("persistedAnswerChanged", !Objects.equals(answerBefore, answerAfter));
        mutation.put("pendingEventDelta", pendingAfter - pendingBefore);

        Map<String, Object> probe = new LinkedHashMap<>();
        probe.put("entrypoint", probeEntrypoint(probeId));
        probe.put("entrypointKind", probeId.startsWith("tournament-quiz-read-")
                ? "COMPOUND_PUBLIC_FACADE_READ_SEQUENCE" : "PUBLIC_APPLICATION_FACADE");
        probe.put("arguments", arguments);
        probe.put("inputSelection", selection);
        probe.put("outcome", outcome);
        probe.put("events", events);
        probe.put("beforeSnapshot", before);
        probe.put("afterSnapshot", after);
        probe.put("pendingEventCountBefore", pendingBefore);
        probe.put("pendingEventCountAfter", pendingAfter);
        probe.put("locksBefore", locksBefore);
        probe.put("locksAfter", locksAfter);
        probe.put("mutationEvidence", mutation);
        if (probeId.startsWith("tournament"))
            probe.put("referenceEvidence", tournamentReferenceEvidence(original, outcome));

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schemaVersion", "microservices-simulator.impact-behavior-probe.v1");
        report.put("probeId", probeId);
        report.put("comparisonRole", probeId.endsWith("-affected") ? "AFFECTED" : "HEALTHY_CONTROL");
        report.put("baseCaseId", baseCase);
        report.put("originalScenario", original);
        report.put("probe", probe);
        report.put("validityChecks", List.of(
                Map.of("name", "original final snapshot captured before probe", "passed", true),
                Map.of("name", "fresh JVM state", "passed", true,
                        "evidence", "One process accepts exactly one probe id and exits after writing its report."),
                Map.of("name", "probe excluded from original timeline and score", "passed", true)));
        report.put("limitations", List.of(
                "Probe mutations and lock changes are a separate continuation and are excluded from originalScenario.finalSnapshot.",
                "The compound Tournament then Quiz facade sequence is not the SolveQuiz workflow and does not establish valid solve preconditions.",
                "Dates are generated identically for matched controls and are not advanced or manipulated.",
                "A probe result characterizes only the recorded public operation and does not define impact or general domain harm."));
        return report;
    }

    private String healthyTournamentControl() {
        actorStatuses.clear();
        actorFunctionalities.clear();
        Aggregate quiz = latest(setup.quizId);
        Aggregate tournament = latest(setup.tournamentId);
        check("healthy Tournament is active", tournament.getState() == Aggregate.AggregateState.ACTIVE,
                Map.of("tournamentKey", key("Tournament", setup.tournamentId)));
        check("healthy referenced Quiz is active", quiz.getState() == Aggregate.AggregateState.ACTIVE,
                Map.of("quizKey", key("Quiz", setup.quizId)));
        return "Identical fixture setup with an active Tournament and Quiz and no removal operation.";
    }

    private Object probeFindTournament(Map<String, Object> arguments) {
        int tournamentId = setup.tournamentId;
        arguments.put("tournamentId", tournamentId);
        TournamentDto dto = context.getBean(TournamentFunctionalities.class).findTournament(tournamentId);
        Map<String, Object> returned = new LinkedHashMap<>();
        returned.put("tournamentId", dto.getAggregateId());
        returned.put("tournamentVersion", dto.getVersion());
        returned.put("state", dto.getState());
        returned.put("quizId", dto.getQuiz() == null ? null : dto.getQuiz().getAggregateId());
        returned.put("quizVersion", dto.getQuiz() == null ? null : dto.getQuiz().getVersion());
        return returned;
    }

    @SuppressWarnings("unchecked")
    private Object probeTournamentThenQuiz(Map<String, Object> arguments) {
        Map<String, Object> tournament = (Map<String, Object>) probeFindTournament(arguments);
        Integer quizId = (Integer) tournament.get("quizId");
        require(quizId != null, "public findTournament returned no Quiz identity");
        arguments.put("quizIdFromFindTournament", quizId);
        var quiz = context.getBean(pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities.QuizFunctionalities.class)
                .findQuiz(quizId);
        Map<String, Object> returned = new LinkedHashMap<>();
        returned.put("findTournament", tournament);
        returned.put("findQuiz", Map.of("quizId", quiz.getAggregateId(), "quizVersion", quiz.getVersion(), "state", quiz.getState()));
        return returned;
    }

    private Object probeAnswerQuestion(Map<String, Object> arguments, Map<String, Object> selection) {
        QuizAnswer answer = latestQuizAnswer();
        require(answer != null, "base case did not produce a QuizAnswer");
        int quizId = answer.getQuiz().getQuizAggregateId();
        int userId = answer.getStudent().getStudentAggregateId();
        Long retainedQuizVersion = answer.getQuiz().getQuizVersion();
        Quiz quiz = (Quiz) aggregateAtVersion(quizId, retainedQuizVersion);
        int questionId = quiz.getQuizQuestions().stream().map(q -> q.getQuestionAggregateId()).min(Integer::compareTo).orElseThrow();
        Question question = (Question) latest(questionId);
        int optionKey = question.getOptions().stream().map(o -> o.getOptionKey()).filter(Objects::nonNull)
                .min(Integer::compareTo).orElseThrow();
        QuestionAnswerDto input = new QuestionAnswerDto();
        input.setSequence(1); input.setQuestionAggregateId(questionId); input.setOptionKey(optionKey); input.setTimeTaken(1);
        arguments.put("quizId", quizId); arguments.put("userId", userId);
        arguments.put("questionAnswer", Map.of("sequence", 1, "questionId", questionId, "optionKey", optionKey, "timeTaken", 1));
        selection.put("source", "QuizAnswer.quiz retained version -> exact persisted Quiz.quizQuestions -> latest persisted Question.options");
        selection.put("policy", "lowest question aggregate id, then lowest non-null option key");
        selection.put("quizAnswerRetainedQuizVersion", retainedQuizVersion);
        selection.put("quizVersion", quiz.getVersion());
        selection.put("questionVersion", question.getVersion());
        context.getBean(QuizAnswerFunctionalities.class).answerQuestion(quizId, userId, input);
        return Map.of("voidReturn", true);
    }

    private QuizAnswer latestQuizAnswer() {
        return tx.execute(status -> {
            entityManager.flush(); entityManager.clear();
            List<QuizAnswer> answers = entityManager.createQuery(
                    "select a from QuizAnswer a where a.version=(select max(b.version) from Aggregate b where b.aggregateId=a.aggregateId)",
                    QuizAnswer.class).getResultList();
            require(answers.size() == 1, "expected one latest QuizAnswer, got " + answers.size());
            return answers.get(0);
        });
    }

    private Map<String, Object> answerObservation() {
        QuizAnswer answer = latestQuizAnswerOrNull();
        if (answer == null) return Map.of("present", false);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("present", true); result.put("key", key("QuizAnswer", answer.getAggregateId()));
        result.put("version", answer.getVersion()); result.put("state", answer.getState().name());
        SagaAggregate sagaAnswer = (SagaAggregate) answer;
        result.put("sagaState", sagaAnswer.getSagaState() == null ? null : sagaAnswer.getSagaState().getStateName());
        result.put("completed", answer.isCompleted()); result.put("questionAnswerCount", answer.getQuestionAnswers().size());
        result.put("answeredQuestionIds", answer.getQuestionAnswers().stream().map(q -> q.getQuestionAggregateId()).sorted().toList());
        return result;
    }

    private QuizAnswer latestQuizAnswerOrNull() {
        return tx.execute(status -> {
            entityManager.flush(); entityManager.clear();
            List<QuizAnswer> answers = entityManager.createQuery(
                    "select a from QuizAnswer a where a.version=(select max(b.version) from Aggregate b where b.aggregateId=a.aggregateId)",
                    QuizAnswer.class).getResultList();
            return answers.isEmpty() ? null : answers.get(0);
        });
    }

    private List<Map<String, Object>> lockEvidence() {
        return tx.execute(status -> {
            entityManager.flush(); entityManager.clear();
            List<Integer> ids = entityManager.createQuery("select distinct a.aggregateId from Aggregate a", Integer.class).getResultList();
            List<Map<String, Object>> locks = new ArrayList<>();
            for (Integer id : new TreeSet<>(ids)) {
                Aggregate aggregate = latestInCurrentTransaction(id);
                if (aggregate instanceof SagaAggregate saga) {
                    Map<String, Object> lock = new LinkedHashMap<>();
                    lock.put("key", aggregate.getClass().getSimpleName() + ":" + id);
                    lock.put("version", aggregate.getVersion());
                    lock.put("sagaState", saga.getSagaState() == null ? null : saga.getSagaState().getStateName());
                    locks.add(lock);
                }
            }
            return locks;
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> tournamentReferenceEvidence(Map<String, Object> original,
                                                                    Map<String, Object> outcome) {
        Map<String, Object> finalSnapshot = (Map<String, Object>) original.get("finalSnapshot");
        List<Map<String, Object>> objects = (List<Map<String, Object>>) finalSnapshot.get("objects");
        Map<String, Object> tournament = objects.stream().filter(o -> "Tournament".equals(o.get("type")))
                .findFirst().orElseThrow();
        List<Map<String, Object>> refs = (List<Map<String, Object>>) tournament.get("refs");
        Map<String, Object> persistedRef = refs.stream().filter(r -> "quiz".equals(r.get("property")))
                .findFirst().orElseThrow();
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("persistedReference", persistedRef);
        evidence.put("persistedReferenceSource", "originalScenario.finalSnapshot Tournament typed persistence getter");
        evidence.put("facadeDtoQuizVersion", facadeTournamentQuizVersion(outcome.get("returned")));
        evidence.put("facadeDtoVersionCaveat", "TournamentFunctionalities.findTournament returned a DTO with null Quiz version; exact targetVersion comes only from the persisted typed reference above.");
        return evidence;
    }

    @SuppressWarnings("unchecked")
    private static Object facadeTournamentQuizVersion(Object returned) {
        if (!(returned instanceof Map<?, ?> map)) return null;
        Object tournament = map.containsKey("findTournament") ? map.get("findTournament") : map;
        return tournament instanceof Map<?, ?> tournamentMap ? tournamentMap.get("quizVersion") : null;
    }

    private static List<Map<String, Object>> exceptionChain(Throwable failure) {
        List<Map<String, Object>> chain = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable current = failure; current != null && seen.add(current); current = current.getCause()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("class", current.getClass().getName()); item.put("message", current.getMessage());
            chain.add(item);
        }
        return chain;
    }

    private static String errorStep(List<Map<String, Object>> events) {
        for (int i = events.size() - 1; i >= 0; i--) {
            Map<String, Object> event = events.get(i);
            if ("STEP_FINISHED".equals(event.get("eventKind"))
                    && "ERROR".equals(((Map<?, ?>) event.get("payload")).get("outcome")))
                return (String) event.get("stepName");
        }
        return null;
    }

    private static Object probeEntrypoint(String probeId) {
        if (probeId.startsWith("tournament-read-")) return "TournamentFunctionalities.findTournament(Integer)";
        if (probeId.startsWith("tournament-quiz-read-")) return List.of(
                "TournamentFunctionalities.findTournament(Integer)", "QuizFunctionalities.findQuiz(Integer)");
        return "QuizAnswerFunctionalities.answerQuestion(Integer,Integer,QuestionAnswerDto)";
    }

    private String createEarlyCompensation() {
        var create = newCreate();
        actorStatuses.put("A", "RUNNING");
        actorFunctionalities.put("A", CreateTournamentFunctionalitySagas.class.getSimpleName());
        actorFunctionalities.put("B", StartQuizFunctionalitySagas.class.getSimpleName());
        action("A", "FORWARD", "create-prefix-through-generateQuizStep", () -> {
            executeCreatePrefix(create); focusIds.add(create.functionality.getQuizDto().getAggregateId());
        });
        int quizId = create.functionality.getQuizDto().getAggregateId();
        action("A", "FORWARD", "createTournamentStep-assigned-fault", () -> {
            Throwable f = injectFault(create.functionality, create.uow, "createTournamentStep", 6, "create-start");
            require(f instanceof FaultVectorInjectedFaultException, "expected assigned fault");
        });
        setLastOutcome("ASSIGNED_FAULT");
        action("A", "COMPENSATION", "resumeCompensation", () -> create.functionality.resumeCompensation(create.uow));
        actorStatuses.put("A", "COMPENSATED");
        actionExpectingFailure("B", "FORWARD", "getQuizStep-read-only-proof-absence", quizId, () -> {
            var start = newStart(quizId);
            start.functionality.executeStepForExecutor("getQuizStep", start.uow);
        });
        actorStatuses.put("B", "FAILED_MISSING_QUIZ");
        Aggregate quiz = latest(quizId);
        check("compensation deleted generated Quiz", quiz.getState() == Aggregate.AggregateState.DELETED,
                Map.of("quizKey", key("Quiz", quizId), "state", quiz.getState().name()));
        check("read-only proof created no QuizAnswer", allAnswerIds().isEmpty(), Map.of("quizAnswerIds", allAnswerIds()));
        return "CreateTournament faults after generating its Quiz, compensates it, then StartQuiz proves the Quiz is absent.";
    }

    private String createSplitStart() {
        var create = newCreate();
        var holder = new Object() { StartHandle start; int quizId; };
        actorStatuses.put("A", "RUNNING");
        actorStatuses.put("B", "RUNNING");
        actorFunctionalities.put("A", CreateTournamentFunctionalitySagas.class.getSimpleName());
        actorFunctionalities.put("B", StartQuizFunctionalitySagas.class.getSimpleName());
        action("A", "FORWARD", "create-prefix-through-generateQuizStep", () -> {
            executeCreatePrefix(create); focusIds.add(create.functionality.getQuizDto().getAggregateId());
        });
        holder.quizId = create.functionality.getQuizDto().getAggregateId();
        action("A", "FORWARD", "createTournamentStep-assigned-fault", () ->
                require(injectFault(create.functionality, create.uow, "createTournamentStep", 6, "create-start")
                        instanceof FaultVectorInjectedFaultException, "expected assigned fault"));
        setLastOutcome("ASSIGNED_FAULT");
        holder.start = newStart(holder.quizId);
        action("B", "FORWARD", "getQuizStep", () -> holder.start.functionality.executeStepForExecutor("getQuizStep", holder.start.uow));
        addReturnedRead("B", holder.quizId, holder.start.functionality.getQuizDto().getVersion());
        action("A", "COMPENSATION", "resumeCompensation", () -> create.functionality.resumeCompensation(create.uow));
        actorStatuses.put("A", "COMPENSATED");
        action("B", "FORWARD", "getUserStep", () -> holder.start.functionality.executeStepForExecutor("getUserStep", holder.start.uow));
        action("B", "FORWARD", "startQuizStep", () -> holder.start.functionality.executeStepForExecutor("startQuizStep", holder.start.uow));
        int answerId = holder.start.functionality.getQuizAnswerDto().getAggregateId();
        focusIds.add(answerId);
        action("B", "FINALIZE", "finalize", () -> require(holder.start.functionality.finalizeForExecutor(holder.start.uow).committed(), "StartQuiz finalization failed"));
        actorStatuses.put("B", "SUCCESS");
        QuizAnswer answer = (QuizAnswer) latest(answerId);
        check("active QuizAnswer retains typed Quiz reference", answer.getState() == Aggregate.AggregateState.ACTIVE
                        && answer.getQuiz().getQuizAggregateId().equals(holder.quizId),
                Map.of("answerKey", key("QuizAnswer", answerId), "quizKey", key("Quiz", holder.quizId)));
        check("referenced Quiz is deleted", latest(holder.quizId).getState() == Aggregate.AggregateState.DELETED,
                Map.of("quizKey", key("Quiz", holder.quizId)));
        return "StartQuiz reads the generated Quiz, then CreateTournament compensation deletes it before StartQuiz completes.";
    }

    private String createSuccessOverlapControl() {
        var create = newCreate();
        var holder = new Object() { StartHandle start; int quizId; };
        actorStatuses.put("A", "RUNNING");
        actorStatuses.put("B", "RUNNING");
        actorFunctionalities.put("A", CreateTournamentFunctionalitySagas.class.getSimpleName());
        actorFunctionalities.put("B", StartQuizFunctionalitySagas.class.getSimpleName());
        action("A", "FORWARD", "create-prefix-through-generateQuizStep", () -> {
            executeCreatePrefix(create); focusIds.add(create.functionality.getQuizDto().getAggregateId());
        });
        holder.quizId = create.functionality.getQuizDto().getAggregateId();
        holder.start = newStart(holder.quizId);
        action("B", "FORWARD", "getQuizStep", () -> holder.start.functionality.executeStepForExecutor("getQuizStep", holder.start.uow));
        addReturnedRead("B", holder.quizId, holder.start.functionality.getQuizDto().getVersion());
        action("A", "FORWARD", "createTournamentStep", () -> create.functionality.executeStepForExecutor("createTournamentStep", create.uow));
        int tournamentId = create.functionality.getTournamentDto().getAggregateId();
        focusIds.add(tournamentId);
        action("A", "FINALIZE", "finalize", () -> require(create.functionality.finalizeForExecutor(create.uow).committed(), "CreateTournament finalization failed"));
        actorStatuses.put("A", "SUCCESS");
        action("B", "FORWARD", "getUserStep", () -> holder.start.functionality.executeStepForExecutor("getUserStep", holder.start.uow));
        action("B", "FORWARD", "startQuizStep", () -> holder.start.functionality.executeStepForExecutor("startQuizStep", holder.start.uow));
        int answerId = holder.start.functionality.getQuizAnswerDto().getAggregateId();
        focusIds.add(answerId);
        action("B", "FINALIZE", "finalize", () -> require(holder.start.functionality.finalizeForExecutor(holder.start.uow).committed(), "StartQuiz finalization failed"));
        actorStatuses.put("B", "SUCCESS");
        check("exposed Quiz remains active", latest(holder.quizId).getState() == Aggregate.AggregateState.ACTIVE,
                Map.of("quizKey", key("Quiz", holder.quizId)));
        check("answer and tournament reference same live Quiz",
                ((QuizAnswer) latest(answerId)).getQuiz().getQuizAggregateId().equals(holder.quizId)
                        && ((Tournament) latest(tournamentId)).getTournamentQuiz().getQuizAggregateId().equals(holder.quizId),
                Map.of("answerKey", key("QuizAnswer", answerId), "tournamentKey", key("Tournament", tournamentId),
                        "quizKey", key("Quiz", holder.quizId)));
        return "StartQuiz observes CreateTournament's unfinished Quiz, but CreateTournament and StartQuiz both finish successfully.";
    }

    private String removeFaultRecoveryAlone() {
        int tournamentId = setup.tournamentId;
        int quizId = setup.quizId;
        focusIds.add(quizId);
        var uow = uowService.createUnitOfWork(RemoveTournamentFunctionalitySagas.class.getSimpleName());
        var remove = new RemoveTournamentFunctionalitySagas(uowService, tournamentId, uow, gateway);
        actorStatuses.put("A", "RUNNING");
        actorFunctionalities.put("A", RemoveTournamentFunctionalitySagas.class.getSimpleName());
        action("A", "FORWARD", "getTournamentStep", () -> remove.executeStepForExecutor("getTournamentStep", uow));
        action("A", "FORWARD", "removeQuizStep", () -> remove.executeStepForExecutor("removeQuizStep", uow));
        action("A", "FORWARD", "removeTournamentStep-assigned-fault", () ->
                require(injectFault(remove, uow, "removeTournamentStep", 2, "remove-alone")
                        instanceof FaultVectorInjectedFaultException, "expected assigned fault"));
        setLastOutcome("ASSIGNED_FAULT");
        action("A", "RECOVERY", "recover-getTournamentStep", () -> {
            var points = remove.recoveryCheckpointsForExecutor(uow);
            require(points.size() == 1 && points.get(0).sourceStepName().equals("getTournamentStep"), "unexpected recovery checkpoints");
            var recovered = remove.recoverStepForExecutor("getTournamentStep", uow);
            require(!recovered.explicitCompensationExecuted() && recovered.implicitRollbackExecuted(), "unexpected recovery mode");
        });
        actorStatuses.put("A", "RECOVERED_PARTIAL");
        Tournament tournament = (Tournament) latest(tournamentId);
        check("Tournament survives recovery", tournament.getState() == Aggregate.AggregateState.ACTIVE,
                Map.of("tournamentKey", key("Tournament", tournamentId)));
        check("no Tournament participant was introduced", tournament.getTournamentParticipants().isEmpty(),
                Map.of("participantCount", tournament.getTournamentParticipants().size()));
        check("surviving Tournament retains typed Quiz reference", tournament.getTournamentQuiz().getQuizAggregateId().equals(quizId),
                Map.of("tournamentKey", key("Tournament", tournamentId), "quizKey", key("Quiz", quizId)));
        check("referenced Quiz is deleted", latest(quizId).getState() == Aggregate.AggregateState.DELETED,
                Map.of("quizKey", key("Quiz", quizId)));
        return "RemoveTournament alone deletes its Quiz, faults before Tournament deletion, then performs its only available recovery.";
    }

    private Setup setupApplication(boolean createTournament) {
        var executionFns = context.getBean(ExecutionFunctionalities.class);
        var userFns = context.getBean(UserFunctionalities.class);
        var topicFns = context.getBean(TopicFunctionalities.class);
        var questionFns = context.getBean(QuestionFunctionalities.class);
        var tournamentFns = context.getBean(TournamentFunctionalities.class);
        LocalDateTime now = LocalDateTime.now();
        CourseExecutionDto course = new CourseExecutionDto();
        course.setName("BLCM"); course.setType("TECNICO"); course.setAcronym("TESTBLCM");
        course.setAcademicTerm("2022/2023"); course.setEndDate(DateHandler.toISOString(now.plusHours(2)));
        course = executionFns.createCourseExecution(course);
        UserDto creator = user("USER_NAME_1", "USER_USERNAME_1", userFns);
        UserDto student = user("USER_NAME_2", "USER_USERNAME_2", userFns);
        executionFns.addStudent(course.getAggregateId(), creator.getAggregateId());
        executionFns.addStudent(course.getAggregateId(), student.getAggregateId());
        TopicDto t1 = topic("TOPIC_NAME_1", course.getCourseAggregateId(), topicFns);
        TopicDto t2 = topic("TOPIC_NAME_2", course.getCourseAggregateId(), topicFns);
        question("Title One", "Content One", "Option One", "Option Two", course.getCourseAggregateId(), t1, questionFns);
        question("Title Two", "Content Two", "Option Three", "Option Four", course.getCourseAggregateId(), t2, questionFns);
        Integer tournamentId = null;
        Integer quizId = null;
        if (createTournament) {
            TournamentDto input = tournamentInput(now);
            TournamentDto made = tournamentFns.createTournament(creator.getAggregateId(), course.getAggregateId(),
                    List.of(t1.getAggregateId(), t2.getAggregateId()), input);
            tournamentId = made.getAggregateId();
            quizId = made.getQuiz().getAggregateId();
        }
        return new Setup(course.getAggregateId(), creator.getAggregateId(), student.getAggregateId(),
                t1.getAggregateId(), t2.getAggregateId(), tournamentId, quizId, now);
    }

    private static UserDto user(String name, String username, UserFunctionalities fns) {
        UserDto dto = new UserDto(); dto.setName(name); dto.setUsername(username); dto.setRole("STUDENT");
        dto = fns.createUser(dto); fns.activateUser(dto.getAggregateId()); return dto;
    }

    private static TopicDto topic(String name, int courseAggregateId, TopicFunctionalities fns) {
        TopicDto dto = new TopicDto(); dto.setName(name); return fns.createTopic(courseAggregateId, dto);
    }

    private static void question(String title, String content, String first, String second,
                                 int courseAggregateId, TopicDto topic, QuestionFunctionalities fns) {
        QuestionDto dto = new QuestionDto(); dto.setTitle(title); dto.setContent(content); dto.setTopicDto(Set.of(topic));
        OptionDto a = new OptionDto(); a.setSequence(1); a.setCorrect(true); a.setContent(first);
        OptionDto b = new OptionDto(); b.setSequence(2); b.setCorrect(false); b.setContent(second);
        dto.setOptionDtos(List.of(a, b)); fns.createQuestion(courseAggregateId, dto);
    }

    private CreateHandle newCreate() {
        SagaUnitOfWork uow = uowService.createUnitOfWork(CreateTournamentFunctionalitySagas.class.getSimpleName());
        var fn = new CreateTournamentFunctionalitySagas(uowService, setup.creatorId, setup.courseId,
                List.of(setup.topic1Id, setup.topic2Id), tournamentInput(setup.now), uow, gateway);
        return new CreateHandle(fn, uow);
    }

    private StartHandle newStart(int quizId) {
        SagaUnitOfWork uow = uowService.createUnitOfWork(StartQuizFunctionalitySagas.class.getSimpleName());
        return new StartHandle(new StartQuizFunctionalitySagas(uowService, quizId, setup.courseId, setup.studentId, uow, gateway), uow);
    }

    private static TournamentDto tournamentInput(LocalDateTime now) {
        TournamentDto dto = new TournamentDto();
        dto.setStartTime(DateHandler.toISOString(now.plusMinutes(5)));
        dto.setEndTime(DateHandler.toISOString(now.plusHours(1)));
        dto.setNumberOfQuestions(2); return dto;
    }

    private static void executeCreatePrefix(CreateHandle create) {
        CREATE_PREFIX.forEach(step -> create.functionality.executeStepForExecutor(step, create.uow));
    }

    private void action(String actor, String phase, String name, ThrowingAction body) {
        recorder.clear();
        String status = "SUCCESS"; String detail = null;
        try { body.run(); } catch (Throwable failure) {
            status = "ERROR"; detail = root(failure).getClass().getName() + ": " + root(failure).getMessage();
            throw failure instanceof RuntimeException r ? r : new RuntimeException(failure);
        } finally { timeline.add(timelineEntry(actor, phase, name, status, detail)); }
    }

    private void actionExpectingFailure(String actor, String phase, String name, int missingId, ThrowingAction body) {
        recorder.clear(); String detail;
        try { body.run(); throw new IllegalStateException("expected action failure"); }
        catch (Throwable failure) {
            Throwable cause = root(failure);
            require(!(cause instanceof IllegalStateException && "expected action failure".equals(cause.getMessage())), "action unexpectedly succeeded");
            require(cause instanceof SimulatorDomainException, "expected missing-aggregate SimulatorDomainException, got " + cause.getClass().getName());
            SimulatorDomainException missing = (SimulatorDomainException) cause;
            require(SimulatorErrorMessage.AGGREGATE_NOT_FOUND.equals(missing.getErrorMessage()),
                    "unexpected simulator error template: " + missing.getErrorMessage());
            require(String.format(SimulatorErrorMessage.AGGREGATE_NOT_FOUND, missingId).equals(missing.getMessage()),
                    "missing-aggregate failure did not exactly identify Quiz " + missingId);
            detail = cause.getClass().getName() + ": " + cause.getMessage();
        }
        timeline.add(timelineEntry(actor, phase, name, "EXPECTED_ERROR", detail));
    }

    @SuppressWarnings("unchecked")
    private void setLastOutcome(String status) {
        ((Map<String, Object>) timeline.get(timeline.size() - 1).get("outcome")).put("status", status);
    }

    private Map<String, Object> timelineEntry(String actor, String phase, String name, String status, String detail) {
        List<Map<String, Object>> events = recorder.drain();
        Map<String, Object> outcome = new LinkedHashMap<>(); outcome.put("status", status); outcome.put("detail", detail);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("index", timeline.size()); row.put("actor", actor); row.put("phase", phase); row.put("action", name);
        row.put("outcome", outcome); row.put("events", events); row.put("pendingEventCount", pendingEvents()); row.put("snapshot", snapshot());
        return row;
    }

    private void addReturnedRead(String actor, int quizId, Long version) {
        timeline.get(timeline.size() - 1).put("returnedReads", List.of(Map.of(
                "actor", actor, "objectKey", key("Quiz", quizId), "version", version,
                "source", "StartQuizFunctionalitySagas.getQuizDto")));
    }

    private Map<String, Object> snapshot() {
        DynamicEvidenceRecorder previous = DynamicEvidenceRecorderHolder.getRecorder();
        DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
        try {
            return tx.execute(status -> {
                entityManager.flush(); entityManager.clear();
                Set<Integer> observedIds = new TreeSet<>();
                observedIds.addAll(context.getBean(pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizRepository.class).findAllAggregateIds());
                observedIds.addAll(context.getBean(pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository.class).findAllAggregateIds());
                observedIds.addAll(context.getBean(QuizAnswerRepository.class).findAllAggregateIds());
                List<Map<String, Object>> objects = new ArrayList<>();
                for (Integer id : observedIds) {
                    Aggregate a = latestInCurrentTransaction(id);
                    if (a != null) objects.add(object(a));
                }
                objects.sort(Comparator.comparing(o -> (String) o.get("key")));
                return Map.of(
                        "coverage", Map.of("complete", true, "types", List.of("Quiz", "Tournament", "QuizAnswer")),
                        "objects", objects);
            });
        } finally { DynamicEvidenceRecorderHolder.setRecorder(previous); }
    }

    private Map<String, Object> object(Aggregate a) {
        String type = a instanceof Quiz ? "Quiz" : a instanceof Tournament ? "Tournament" :
                a instanceof QuizAnswer ? "QuizAnswer" : a.getClass().getSimpleName();
        List<Map<String, Object>> refs = new ArrayList<>();
        if (a instanceof Tournament t && t.getTournamentQuiz() != null)
            refs.add(Map.of("property", "quiz", "targetKey", key("Quiz", t.getTournamentQuiz().getQuizAggregateId()),
                    "targetVersion", t.getTournamentQuiz().getQuizVersion()));
        if (a instanceof QuizAnswer q && q.getQuiz() != null)
            refs.add(Map.of("property", "quiz", "targetKey", key("Quiz", q.getQuiz().getQuizAggregateId()),
                    "targetVersion", q.getQuiz().getQuizVersion()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("key", key(type, a.getAggregateId())); out.put("type", type); out.put("runtimeType", a.getClass().getSimpleName());
        out.put("id", a.getAggregateId()); out.put("version", a.getVersion()); out.put("state", a.getState().name());
        out.put("previousVersion", a.getPrev() == null ? null : a.getPrev().getVersion());
        out.put("sagaState", a instanceof SagaAggregate s && s.getSagaState() != null ? s.getSagaState().getStateName() : null);
        out.put("refs", refs); return out;
    }

    private Aggregate latest(int id) { return tx.execute(status -> { entityManager.flush(); entityManager.clear(); return latestInCurrentTransaction(id); }); }
    private Aggregate aggregateAtVersion(int id, long version) {
        return tx.execute(status -> {
            entityManager.flush(); entityManager.clear();
            return entityManager.createQuery("select a from Aggregate a where a.aggregateId=:id and a.version=:version", Aggregate.class)
                    .setParameter("id", id).setParameter("version", version).getSingleResult();
        });
    }
    private Aggregate latestInCurrentTransaction(int id) {
        List<Aggregate> rows = entityManager.createQuery("select a from Aggregate a where a.aggregateId=:id order by a.version desc", Aggregate.class)
                .setParameter("id", id).setMaxResults(1).getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Set<Integer> allAnswerIds() { return tx.execute(s -> new TreeSet<>(context.getBean(QuizAnswerRepository.class).findAllAggregateIds())); }
    private int pendingEvents() { return Math.toIntExact(eventService.eventCountForReplay()); }

    private void check(String name, boolean passed, Object evidence) {
        Map<String, Object> c = new LinkedHashMap<>(); c.put("name", name); c.put("passed", passed); c.put("evidence", evidence); checks.add(c);
    }

    private static Throwable injectFault(Object fn, SagaUnitOfWork uow, String step, int slot, String plan) {
        var functionality = (pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality) fn;
        var boundary = new FaultVectorBoundaryContext("impact-experiment", plan, "A", step + "-occurrence", slot,
                fn.getClass().getName(), fn.getClass().getSimpleName(), step, 1);
        try (var provider = FaultVectorProviderHolder.install(new InMemoryFaultVectorProvider(Map.of(slot, FaultVectorFault.from(boundary))));
             var scope = FaultVectorProviderHolder.enterBoundary(boundary)) {
            functionality.executeStepForExecutor(step, uow);
            throw new AssertionError("expected assigned fault at " + step);
        } catch (CompletionException failure) { return root(failure); }
    }

    private static String key(String type, int id) { return type + ":" + id; }
    private static Throwable root(Throwable t) { while (t instanceof CompletionException && t.getCause() != null) t = t.getCause(); return t; }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }

    private record Setup(int courseId, int creatorId, int studentId, int topic1Id, int topic2Id,
                         Integer tournamentId, Integer quizId, LocalDateTime now) {}
    private record CreateHandle(CreateTournamentFunctionalitySagas functionality, SagaUnitOfWork uow) {}
    private record StartHandle(StartQuizFunctionalitySagas functionality, SagaUnitOfWork uow) {}
    @FunctionalInterface private interface ThrowingAction { void run() throws Exception; }

    private static final class RecordingRecorder implements DynamicEvidenceRecorder {
        private final List<DynamicEvidenceEvent> events = new ArrayList<>();
        public boolean isEnabled() { return true; }
        public synchronized void record(DynamicEvidenceEvent event) { events.add(event); }
        public void close() {}
        synchronized void clear() { events.clear(); }
        @SuppressWarnings("unchecked")
        synchronized List<Map<String, Object>> drain() {
            List<Map<String, Object>> result = events.stream()
                    .map(e -> (Map<String, Object>) JSON.convertValue(e, Map.class)).toList();
            events.clear(); return result;
        }
    }
}
