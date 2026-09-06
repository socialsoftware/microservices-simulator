package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.experiments.updates;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorBoundaryContext;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorFault;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorInjectedFaultException;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorProviderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.InMemoryFaultVectorProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceNoopRecorder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventReplayCoordinator;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventReplayException;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.notification.handling.QuizEventHandling;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.notification.handling.handlers.UpdateQuestionEventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Disposable application-side observer for the bounded update-impact experiment. */
public final class ImpactUpdatesExperiment {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final ConfigurableApplicationContext context;
    private final TransactionTemplate tx;
    private final EntityManager entityManager;
    private final EventService eventService;
    private final SagaUnitOfWorkService uowService;
    private final LocalCommandGateway gateway;
    private final RecordingRecorder recorder = new RecordingRecorder();
    private final List<Map<String, Object>> actions = new ArrayList<>();
    private final List<Map<String, Object>> checks = new ArrayList<>();
    private final Map<String, Object> report = new LinkedHashMap<>();

    private ImpactUpdatesExperiment(ConfigurableApplicationContext context, String caseId) {
        this.context = context;
        this.tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        this.entityManager = context.getBean(EntityManager.class);
        this.eventService = context.getBean(EventService.class);
        this.uowService = context.getBean(SagaUnitOfWorkService.class);
        this.gateway = context.getBean(LocalCommandGateway.class);
        report.put("schemaVersion", "microservices-simulator.impact-updates-experiment.v1");
        report.put("caseId", caseId);
        report.put("build", Map.of(
                "variant", System.getProperty("experiment.variant", "unknown"),
                "sourceRevision", System.getProperty("experiment.sourceRevision", "unknown"),
                "originalQuizServiceSha256", System.getProperty("experiment.quizServiceSha256", "unknown"),
                "originalUpdateTournamentSagaSha256", System.getProperty("experiment.updateTournamentSha256", "unknown"),
                "variantQuizServiceSha256", System.getProperty("experiment.variantQuizServiceSha256", "unknown"),
                "variantUpdateTournamentSagaSha256", System.getProperty("experiment.variantUpdateTournamentSha256", "unknown"),
                "fixtureNow", System.getProperty("experiment.fixtureNow", "unknown"),
                "appliedPatchSha256", System.getProperty("experiment.patchSha256", "none")));
        report.put("freshJvm", true);
        report.put("actions", actions);
        report.put("checks", checks);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: ImpactUpdatesExperiment CASE_ID OUTPUT_PATH");
        String caseId = args[0];
        Path output = Path.of(args[1]);
        System.setProperty("spring.profiles.active", "test,sagas,local");
        System.setProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY, "true");
        ImpactUpdatesExperiment experiment = null;
        Throwable failure = null;
        try (var replay = EventReplayCoordinator.activate();
             var context = SpringApplication.run(QuizzesSimulator.class,
                     "--verifiers.application.enabled=false", "--server.port=0", "--spring.main.banner-mode=off")) {
            experiment = new ImpactUpdatesExperiment(context, caseId);
            DynamicEvidenceRecorderHolder.setRecorder(experiment.recorder);
            if (caseId.startsWith("question-")) experiment.runQuestionCase(caseId);
            else if (caseId.startsWith("tournament-")) experiment.runTournamentCase(caseId);
            else throw new IllegalArgumentException("unknown case " + caseId);
            experiment.assertChecks();
            experiment.report.put("status", "PASS");
        } catch (Throwable caught) {
            failure = root(caught);
            if (experiment == null) {
                experiment = new ImpactUpdatesExperimentStub(caseId).delegate;
            }
            experiment.report.put("status", "FAIL");
            experiment.report.put("failure", failure(failure));
        } finally {
            DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
            FaultVectorProviderHolder.clear();
            if (experiment != null) JSON.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), experiment.report);
        }
        if (failure != null) throw failure instanceof Exception e ? e : new RuntimeException(failure);
        System.out.println("IMPACT_UPDATES_EXPERIMENT_PASS " + caseId);
    }

    /* Used only to retain a report if Spring startup itself fails before the real observer exists. */
    private static final class ImpactUpdatesExperimentStub {
        private final ImpactUpdatesExperiment delegate;
        private ImpactUpdatesExperimentStub(String caseId) {
            this.delegate = new ImpactUpdatesExperiment(caseId);
        }
    }

    private ImpactUpdatesExperiment(String caseId) {
        context = null; tx = null; entityManager = null; eventService = null; uowService = null; gateway = null;
        report.put("schemaVersion", "microservices-simulator.impact-updates-experiment.v1");
        report.put("caseId", caseId); report.put("freshJvm", true); report.put("actions", actions); report.put("checks", checks);
    }

    private void runQuestionCase(String caseId) {
        Setup setup = setup(true);
        int setupEvents = Math.toIntExact(eventService.eventCountForReplay());
        eventService.clearEventsForReplay();
        check("setup events cleared before measurement", eventService.eventCountForReplay() == 0,
                Map.of("clearedRawStoredEventCount", setupEvents));

        int questionId = tx.execute(status -> {
            Quiz quiz = (Quiz) latestInTx(setup.quizId());
            return quiz.getQuizQuestions().stream().map(QuizQuestion::getQuestionAggregateId).min(Integer::compareTo).orElseThrow();
        });
        Map<String, Object> initial = questionPair(questionId, setup.quizId());
        report.put("comparisonRole", caseId.equals("question-baseline") ? "CURRENT_APPLICATION" : "REPAIRED_DIAGNOSTIC_CONTROL");
        report.put("observationBoundary", "Exact captured UpdateQuestionEvent delivered through the real Quiz event handler; unscoped scheduled polling suppressed by replay mode.");
        report.put("initial", initial);

        QuestionDto update = tx.execute(status -> {
            Question current = (Question) latestInTx(questionId);
            return new QuestionDto(current);
        });
        update.setTitle("UPDATED QUESTION TITLE");
        update.setContent("UPDATED QUESTION CONTENT");
        EventReplayCoordinator.CapturedEvent captured;
        recorder.clear();
        try (var capture = EventReplayCoordinator.beginTriggerCapture("question-update")) {
            context.getBean(QuestionFunctionalities.class).updateQuestion(update);
            List<EventReplayCoordinator.CapturedEvent> found = capture.capturedEvents().stream()
                    .filter(e -> e.eventTypeFqn().equals(UpdateQuestionEvent.class.getName())).toList();
            if (found.size() != 1) throw new IllegalStateException("expected one captured UpdateQuestionEvent, got " + found.size());
            captured = found.get(0);
        }
        action("update-question-public-facade", "SUCCESS", null, Map.of(
                "entrypoint", QuestionFunctionalities.class.getName() + ".updateQuestion",
                "capturedEvent", capturedEvent(captured),
                "state", questionPair(questionId, setup.quizId())), recorder.drain());
        Map<String, Object> afterPublisher = questionPair(questionId, setup.quizId());

        Delivery first = deliverQuestionEvent(captured, "deliver-update-question-first", false);
        Map<String, Object> afterFirst = questionPair(questionId, setup.quizId());
        Delivery second = deliverQuestionEvent(captured, "deliver-update-question-second", caseId.equals("question-repaired"));
        Map<String, Object> afterSecond = questionPair(questionId, setup.quizId());
        report.put("event", capturedEvent(captured));
        report.put("deliveryRoute", Map.of("eventType", UpdateQuestionEvent.class.getName(),
                "handlerClass", UpdateQuestionEventHandler.class.getName(),
                "entrypoint", QuizEventHandling.class.getName() + ".handleUpdateQuestionEvent"));
        report.put("afterPublisher", afterPublisher);
        report.put("firstDelivery", first.asMap());
        report.put("afterFirstDelivery", afterFirst);
        report.put("secondDelivery", second.asMap());
        report.put("afterSecondDelivery", afterSecond);
        report.put("rawStoredEventCountAtHorizon", eventService.eventCountForReplay());
        report.put("rawStoredEventCountMeaning", "Event rows are retained; this is not a pending-event count. Eligibility is determined by receiver subscription version.");

        Map<String, Object> qAfter = castMap(afterPublisher.get("question"));
        Map<String, Object> quizBefore = castMap(initial.get("quizQuestion"));
        Map<String, Object> quizAfter1 = castMap(afterFirst.get("quizQuestion"));
        Map<String, Object> quizAfter2 = castMap(afterSecond.get("quizQuestion"));
        check("publisher persisted requested values", update.getTitle().equals(qAfter.get("title")) && update.getContent().equals(qAfter.get("content")), qAfter);
        check("captured event has exact publisher, identity, publication state and values",
                captured.eventId() != null && captured.eventId() > 0 && captured.published()
                        && captured.publisherAggregateId().equals(questionId)
                        && update.getTitle().equals(castMap(capturedEvent(captured).get("payload")).get("title"))
                        && update.getContent().equals(castMap(capturedEvent(captured).get("payload")).get("content")),
                capturedEvent(captured));
        check("first delivery selected exact receiver and returned successfully", first.success && first.subscriberId != null && first.subscriberId == setup.quizId(), first.asMap());
        check("observed Question and Quiz locks released after delivery",
                "NOT_IN_SAGA".equals(castMap(afterFirst.get("question")).get("sagaState"))
                        && "NOT_IN_SAGA".equals(castMap(afterFirst.get("quiz")).get("sagaState")),
                Map.of("questionSagaState", castMap(afterFirst.get("question")).get("sagaState"),
                        "quizSagaState", castMap(afterFirst.get("quiz")).get("sagaState")));
        if (caseId.equals("question-baseline")) {
            check("receiver made no durable progress after first delivery", quizBefore.equals(quizAfter1), Map.of("before", quizBefore, "after", quizAfter1));
            check("receiver aggregate version remained unchanged after completed delivery",
                    castMap(initial.get("quiz")).get("version").equals(castMap(afterFirst.get("quiz")).get("version")),
                    Map.of("beforeVersion", castMap(initial.get("quiz")).get("version"), "afterVersion", castMap(afterFirst.get("quiz")).get("version")));
            check("same exact event remained eligible and returned successfully again", second.success && second.subscriberId != null && second.subscriberId == setup.quizId(), second.asMap());
            check("receiver remained unchanged after repeated delivery", quizBefore.equals(quizAfter2), Map.of("before", quizBefore, "after", quizAfter2));
        } else {
            check("repaired receiver converged to publisher values and event version",
                    update.getTitle().equals(quizAfter1.get("title")) && update.getContent().equals(quizAfter1.get("content"))
                            && captured.publisherAggregateVersion().equals(((Number) quizAfter1.get("questionVersion")).longValue()), quizAfter1);
            check("repaired receiver advanced durable aggregate version",
                    ((Number) castMap(afterFirst.get("quiz")).get("version")).longValue() > ((Number) castMap(initial.get("quiz")).get("version")).longValue(),
                    Map.of("before", initial.get("quiz"), "after", afterFirst.get("quiz")));
            check("same exact event became ineligible after durable progress",
                    !second.success && EventReplayException.class.getName().equals(second.failureClass)
                            && "SELECTED_SUBSCRIBER_NOT_FOUND".equals(second.failureReason), second.asMap());
        }
        report.put("limitations", List.of(
                "The current application result is a reproduced persistence-progress failure, not a domain-harm or severity score.",
                "The repaired build differs by the recorded one-line registerChanged patch and exists only in the temporary build directory.",
                "Raw event-row retention is reported separately from delivery eligibility."));
    }

    private Delivery deliverQuestionEvent(EventReplayCoordinator.CapturedEvent event, String actionName, boolean expectIneligible) {
        recorder.clear();
        Integer subscriber = null;
        boolean success = false;
        Throwable caught = null;
        try (var selection = EventReplayCoordinator.beginSelectedEvent(event, UpdateQuestionEvent.class.getName(), UpdateQuestionEventHandler.class.getName())) {
            context.getBean(QuizEventHandling.class).handleUpdateQuestionEvent();
            selection.verifyCompleted();
            subscriber = selection.subscriberAggregateId();
            success = true;
        } catch (Throwable failure) {
            caught = root(failure);
        }
        List<Map<String, Object>> evidence = recorder.drain();
        String reason = caught instanceof EventReplayException replayFailure ? replayFailure.reason() : null;
        boolean exactExpectedIneligible = expectIneligible && caught instanceof EventReplayException
                && "SELECTED_SUBSCRIBER_NOT_FOUND".equals(reason);
        action(actionName, success ? "SUCCESS" : exactExpectedIneligible ? "EXPECTED_INELIGIBLE" : "ERROR", caught,
                Map.of("capturedEvent", capturedEvent(event), "eventReplayReason", reason == null ? "" : reason), evidence);
        return new Delivery(success, subscriber, caught == null ? null : caught.getClass().getName(),
                caught == null ? null : caught.getMessage(), reason, evidence);
    }

    private void runTournamentCase(String caseId) {
        Setup setup = setup(true);
        int setupEvents = Math.toIntExact(eventService.eventCountForReplay());
        eventService.clearEventsForReplay();
        check("setup events cleared before measurement", eventService.eventCountForReplay() == 0,
                Map.of("clearedRawStoredEventCount", setupEvents));
        report.put("comparisonRole", caseId.equals("tournament-normal-compensation") ? "CURRENT_APPLICATION" : "CONTROLLED_NOOP_COMPENSATION_MUTANT");
        report.put("observationBoundary", "One UpdateTournament Saga writes Tournament, then receives an assigned pre-body fault at updateQuizStep and executes every reported recovery checkpoint.");

        Map<String, Object> initial = tournamentPair(setup.tournamentId(), setup.quizId());
        report.put("initial", initial);
        TournamentDto input = new TournamentDto();
        input.setAggregateId(setup.tournamentId());
        input.setStartTime(DateHandler.toISOString(setup.now().plusMinutes(25)));
        input.setEndTime(DateHandler.toISOString(setup.now().plusHours(1).plusMinutes(25)));
        input.setNumberOfQuestions(3);
        Set<Integer> topicIds = Set.of(setup.topic1Id(), setup.topic2Id(), setup.topic3Id());
        List<Integer> requestedTopicIds = topicIds.stream().sorted().toList();
        String requestedStart = DateHandler.toLocalDateTime(input.getStartTime()).toString();
        String requestedEnd = DateHandler.toLocalDateTime(input.getEndTime()).toString();
        report.put("requestedTournamentUpdate", Map.of("numberOfQuestions", 3, "startTime", requestedStart,
                "endTime", requestedEnd, "wireStartTime", input.getStartTime(), "wireEndTime", input.getEndTime(),
                "topicIds", requestedTopicIds));
        SagaUnitOfWork uow = uowService.createUnitOfWork(UpdateTournamentFunctionalitySagas.class.getSimpleName());
        var update = new UpdateTournamentFunctionalitySagas(uowService, input, topicIds, uow, gateway);

        executeSagaStep(update, uow, "getOriginalTournamentStep");
        executeSagaStep(update, uow, "getTopicsStep");
        executeSagaStep(update, uow, "updateTournamentStep");
        Map<String, Object> afterForward = tournamentPair(setup.tournamentId(), setup.quizId());
        report.put("afterForwardTournamentWrite", afterForward);
        executeSagaStep(update, uow, "findQuestionsByTopicIds");

        recorder.clear();
        var boundary = new FaultVectorBoundaryContext("impact-updates", caseId, "A", "updateQuizStep#0", 0,
                UpdateTournamentFunctionalitySagas.class.getName(), UpdateTournamentFunctionalitySagas.class.getSimpleName(), "updateQuizStep", 1);
        pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowStepExecutionResult faultResult;
        try (var provider = FaultVectorProviderHolder.install(new InMemoryFaultVectorProvider(Map.of(0, FaultVectorFault.from(boundary))));
             var ignored = FaultVectorProviderHolder.enterBoundary(boundary)) {
            faultResult = update.executeStepForExecutorControlled("updateQuizStep", uow);
        }
        Throwable injected = root(faultResult.failure());
        boolean exactFault = injected instanceof FaultVectorInjectedFaultException fault
                && fault.getSlotIndex() == boundary.slotIndex() && fault.getAssignedBit() == boundary.assignedBit()
                && boundary.scenarioExecutionId().equals(fault.getScenarioExecutionId())
                && boundary.scenarioPlanId().equals(fault.getScenarioPlanId())
                && boundary.sagaInstanceId().equals(fault.getSagaInstanceId())
                && boundary.scheduledStepId().equals(fault.getScheduledStepId())
                && boundary.functionalityClassFqn().equals(fault.getFunctionalityClassFqn())
                && boundary.functionalityClassSimpleName().equals(fault.getFunctionalityClassSimpleName())
                && boundary.runtimeStepName().equals(fault.getRuntimeStepName());
        action("updateQuizStep-assigned-pre-body-fault", exactFault ? "ASSIGNED_FAULT" : "ERROR",
                faultResult.failure(), Map.of("boundary", boundary), recorder.drain());
        Map<String, Object> afterFault = tournamentPair(setup.tournamentId(), setup.quizId());
        report.put("afterAssignedFaultBeforeRecovery", afterFault);
        check("exact assigned fault stopped Quiz update before body", !faultResult.completed() && exactFault, failure(faultResult.failure()));
        check("fault boundary produced no durable Quiz body write", initial.get("quiz").equals(afterFault.get("quiz")),
                Map.of("before", initial.get("quiz"), "afterFault", afterFault.get("quiz")));

        List<Map<String, Object>> checkpointPlan = update.recoveryCheckpointsForExecutor(uow).stream().map(cp -> Map.<String, Object>of(
                "sourceStep", cp.sourceStepName(), "explicitCompensationPending", cp.explicitCompensationPending(),
                "implicitRollbackPending", cp.implicitRollbackPending())).toList();
        report.put("recoveryCheckpointPlan", checkpointPlan);
        List<Map<String, Object>> recoveryResults = new ArrayList<>();
        for (var checkpoint : update.recoveryCheckpointsForExecutor(uow)) {
            recorder.clear();
            var result = update.recoverStepForExecutor(checkpoint.sourceStepName(), uow);
            Map<String, Object> value = Map.of("sourceStep", result.sourceStepName(),
                    "explicitCompensationExecuted", result.explicitCompensationExecuted(),
                    "implicitRollbackExecuted", result.implicitRollbackExecuted());
            recoveryResults.add(value);
            action("recover-" + checkpoint.sourceStepName(), "SUCCESS", null, value, recorder.drain());
        }
        report.put("recoveryResults", recoveryResults);
        Map<String, Object> finalState = tournamentPair(setup.tournamentId(), setup.quizId());
        report.put("final", finalState);
        report.put("rawStoredEventCountAtHorizon", eventService.eventCountForReplay());

        Map<String, Object> initialTournament = castMap(initial.get("tournament"));
        Map<String, Object> forwardTournament = castMap(afterForward.get("tournament"));
        Map<String, Object> finalTournament = castMap(finalState.get("tournament"));
        Map<String, Object> initialQuiz = castMap(initial.get("quiz"));
        Map<String, Object> finalQuiz = castMap(finalState.get("quiz"));
        check("forward Tournament write persisted exact requested selected projection",
                ((Number) forwardTournament.get("numberOfQuestions")).intValue() == 3
                        && requestedTopicIds.equals(forwardTournament.get("topicIds"))
                        && requestedStart.equals(forwardTournament.get("startTime"))
                        && requestedEnd.equals(forwardTournament.get("endTime")),
                Map.of("requested", report.get("requestedTournamentUpdate"), "observed", forwardTournament));
        check("recovery checkpoints and execution modes are exact",
                checkpointPlan.size() == 2 && recoveryResults.size() == 2
                        && checkpoint("updateTournamentStep", true, false, checkpointPlan)
                        && checkpoint("getOriginalTournamentStep", false, true, checkpointPlan)
                        && recovery("updateTournamentStep", true, false, recoveryResults)
                        && recovery("getOriginalTournamentStep", false, true, recoveryResults),
                Map.of("plan", checkpointPlan, "results", recoveryResults));
        check("faulted Quiz step made no durable Quiz change", initialQuiz.equals(finalQuiz), Map.of("before", initialQuiz, "after", finalQuiz));
        check("observed Tournament and Quiz latest locks released", "NOT_IN_SAGA".equals(finalTournament.get("sagaState")) && "NOT_IN_SAGA".equals(finalQuiz.get("sagaState")),
                Map.of("tournament", finalTournament.get("sagaState"), "quiz", finalQuiz.get("sagaState")));
        if (caseId.equals("tournament-normal-compensation")) {
            check("normal compensation restored selected business projection",
                    sameTournamentBusinessProjection(initialTournament, finalTournament), Map.of("initial", initialTournament, "final", finalTournament));
            check("normal compensation produced a newer durable version rather than byte equality",
                    ((Number) finalTournament.get("version")).longValue() > ((Number) forwardTournament.get("version")).longValue(),
                    Map.of("forwardVersion", forwardTournament.get("version"), "finalVersion", finalTournament.get("version")));
        } else {
            check("no-op compensation left the failed forward projection durable",
                    sameTournamentBusinessProjection(forwardTournament, finalTournament), Map.of("forward", forwardTournament, "final", finalTournament));
            check("mutant retained update residual relative to initial projection",
                    !sameTournamentBusinessProjection(initialTournament, finalTournament), Map.of("initial", initialTournament, "final", finalTournament));
        }
        report.put("limitations", List.of(
                "The no-op compensation is an explicit controlled mutant in a temporary copied build, not an existing Quizzes defect.",
                "Selected business projections retain the Tournament's domain dates and exclude framework creationTs, version, previous-version link and Saga lock state; recovery is not expected to restore byte equality.",
                "The isolated run attributes the residual to one failed Saga; it does not test concurrent writers or general lost-update detection."));
    }

    private void executeSagaStep(UpdateTournamentFunctionalitySagas update, SagaUnitOfWork uow, String step) {
        recorder.clear();
        Throwable failure = null;
        try { update.executeStepForExecutor(step, uow); }
        catch (Throwable caught) { failure = root(caught); }
        action(step, failure == null ? "SUCCESS" : "ERROR", failure, Map.of(), recorder.drain());
        if (failure != null) throw failure instanceof RuntimeException r ? r : new RuntimeException(failure);
    }

    private Setup setup(boolean withTournament) {
        DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
        var executionFns = context.getBean(ExecutionFunctionalities.class);
        var userFns = context.getBean(UserFunctionalities.class);
        var topicFns = context.getBean(TopicFunctionalities.class);
        var questionFns = context.getBean(QuestionFunctionalities.class);
        var tournamentFns = context.getBean(TournamentFunctionalities.class);
        LocalDateTime now = LocalDateTime.parse(System.getProperty("experiment.fixtureNow",
                LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).toString()));
        CourseExecutionDto course = new CourseExecutionDto();
        course.setName("BLCM"); course.setType("TECNICO"); course.setAcronym("TESTBLCM");
        course.setAcademicTerm("2022/2023"); course.setEndDate(DateHandler.toISOString(now.plusHours(2)));
        course = executionFns.createCourseExecution(course);
        UserDto creator = new UserDto(); creator.setName("CREATOR"); creator.setUsername("creator"); creator.setRole("STUDENT");
        creator = userFns.createUser(creator); userFns.activateUser(creator.getAggregateId());
        executionFns.addStudent(course.getAggregateId(), creator.getAggregateId());
        TopicDto t1 = topic("TOPIC 1", course.getCourseAggregateId(), topicFns);
        TopicDto t2 = topic("TOPIC 2", course.getCourseAggregateId(), topicFns);
        TopicDto t3 = topic("TOPIC 3", course.getCourseAggregateId(), topicFns);
        question("Question 1", "Content 1", course.getCourseAggregateId(), t1, questionFns);
        question("Question 2", "Content 2", course.getCourseAggregateId(), t2, questionFns);
        question("Question 3", "Content 3", course.getCourseAggregateId(), t3, questionFns);
        Integer tournamentId = null, quizId = null;
        if (withTournament) {
            TournamentDto input = new TournamentDto();
            input.setStartTime(DateHandler.toISOString(now.plusMinutes(5)));
            input.setEndTime(DateHandler.toISOString(now.plusHours(1)));
            input.setNumberOfQuestions(2);
            TournamentDto made = tournamentFns.createTournament(creator.getAggregateId(), course.getAggregateId(),
                    List.of(t1.getAggregateId(), t2.getAggregateId()), input);
            tournamentId = made.getAggregateId(); quizId = made.getQuiz().getAggregateId();
        }
        DynamicEvidenceRecorderHolder.setRecorder(recorder);
        return new Setup(course.getAggregateId(), creator.getAggregateId(), t1.getAggregateId(), t2.getAggregateId(), t3.getAggregateId(), tournamentId, quizId, now);
    }

    private static TopicDto topic(String name, int courseId, TopicFunctionalities fns) {
        TopicDto dto = new TopicDto(); dto.setName(name); return fns.createTopic(courseId, dto);
    }

    private static void question(String title, String content, int courseId, TopicDto topic, QuestionFunctionalities fns) {
        QuestionDto dto = new QuestionDto(); dto.setTitle(title); dto.setContent(content); dto.setTopicDto(Set.of(topic));
        OptionDto a = new OptionDto(); a.setSequence(1); a.setCorrect(true); a.setContent("A");
        OptionDto b = new OptionDto(); b.setSequence(2); b.setCorrect(false); b.setContent("B");
        dto.setOptionDtos(List.of(a, b)); fns.createQuestion(courseId, dto);
    }

    private Map<String, Object> questionPair(int questionId, int quizId) {
        return tx.execute(status -> {
            entityManager.flush(); entityManager.clear();
            Question q = (Question) latestInTx(questionId);
            Quiz quiz = (Quiz) latestInTx(quizId);
            QuizQuestion qq = quiz.findQuestion(questionId);
            List<Map<String, Object>> questions = quiz.getQuizQuestions().stream()
                    .sorted(Comparator.comparing(QuizQuestion::getQuestionAggregateId))
                    .map(v -> Map.<String, Object>of("questionId", v.getQuestionAggregateId(), "questionVersion", v.getQuestionVersion(),
                            "title", v.getTitle(), "content", v.getContent(), "state", v.getState().name())).toList();
            Map<String, Object> pair = new LinkedHashMap<>();
            pair.put("question", aggregateEnvelope(q, Map.of("title", q.getTitle(), "content", q.getContent())));
            pair.put("quiz", aggregateEnvelope(quiz, Map.of("questionCount", quiz.getQuizQuestions().size(), "questions", questions)));
            pair.put("quizQuestion", qq == null ? null : Map.of("questionId", qq.getQuestionAggregateId(),
                    "questionVersion", qq.getQuestionVersion(), "title", qq.getTitle(), "content", qq.getContent()));
            return pair;
        });
    }

    private Map<String, Object> tournamentPair(int tournamentId, int quizId) {
        return tx.execute(status -> {
            entityManager.flush(); entityManager.clear();
            Tournament t = (Tournament) latestInTx(tournamentId);
            Quiz q = (Quiz) latestInTx(quizId);
            List<Integer> topicIds = t.getTournamentTopics().stream().map(v -> v.getTopicAggregateId()).sorted().toList();
            List<Map<String, Object>> topics = t.getTournamentTopics().stream()
                    .sorted(Comparator.comparing(v -> v.getTopicAggregateId()))
                    .map(v -> Map.<String, Object>of("topicId", v.getTopicAggregateId(), "topicVersion", v.getTopicVersion(),
                            "name", v.getTopicName(), "state", v.getState().name())).toList();
            List<Map<String, Object>> questions = q.getQuizQuestions().stream()
                    .sorted(Comparator.comparing(QuizQuestion::getQuestionAggregateId))
                    .map(v -> Map.<String, Object>of("questionId", v.getQuestionAggregateId(), "questionVersion", v.getQuestionVersion(),
                            "title", v.getTitle(), "content", v.getContent(), "state", v.getState().name())).toList();
            Map<String, Object> tournamentData = new LinkedHashMap<>();
            tournamentData.put("numberOfQuestions", t.getNumberOfQuestions());
            tournamentData.put("startTime", t.getStartTime().toString());
            tournamentData.put("endTime", t.getEndTime().toString());
            tournamentData.put("topicIds", topicIds);
            tournamentData.put("topics", topics);
            tournamentData.put("quizReference", Map.of("quizId", t.getTournamentQuiz().getQuizAggregateId(),
                    "quizVersion", t.getTournamentQuiz().getQuizVersion()));
            Map<String, Object> pair = new LinkedHashMap<>();
            pair.put("tournament", aggregateEnvelope(t, tournamentData));
            pair.put("quiz", aggregateEnvelope(q, Map.of("questionCount", q.getQuizQuestions().size(),
                    "availableDate", q.getAvailableDate().toString(), "conclusionDate", q.getConclusionDate().toString(),
                    "questions", questions)));
            return pair;
        });
    }

    private Map<String, Object> aggregateEnvelope(Aggregate aggregate, Map<String, Object> business) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("aggregateId", aggregate.getAggregateId()); out.put("version", aggregate.getVersion());
        out.put("previousVersion", aggregate.getPrev() == null ? null : aggregate.getPrev().getVersion());
        out.put("state", aggregate.getState().name());
        out.put("sagaState", aggregate instanceof SagaAggregate saga && saga.getSagaState() != null ? saga.getSagaState().getStateName() : null);
        out.putAll(business); return out;
    }

    private Aggregate latestInTx(int id) {
        List<Aggregate> rows = entityManager.createQuery("select a from Aggregate a where a.aggregateId=:id order by a.version desc", Aggregate.class)
                .setParameter("id", id).setMaxResults(1).getResultList();
        if (rows.isEmpty()) throw new IllegalStateException("aggregate not found " + id);
        return rows.get(0);
    }

    private void action(String name, String status, Throwable failure, Map<String, Object> details, List<Map<String, Object>> evidence) {
        Map<String, Object> row = new LinkedHashMap<>(); row.put("index", actions.size()); row.put("name", name); row.put("status", status);
        row.put("failure", failure(failure)); row.put("details", details); row.put("dynamicEvidence", evidence); actions.add(row);
    }

    private void check(String name, boolean passed, Object evidence) {
        checks.add(Map.of("name", name, "passed", passed, "evidence", evidence));
    }

    private void assertChecks() {
        List<String> failed = checks.stream().filter(c -> !Boolean.TRUE.equals(c.get("passed"))).map(c -> (String) c.get("name")).toList();
        if (!failed.isEmpty()) throw new IllegalStateException("failed checks: " + failed);
    }

    private Map<String, Object> capturedEvent(EventReplayCoordinator.CapturedEvent event) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("eventId", event.eventId()); out.put("eventType", event.eventTypeFqn());
        out.put("publisherAggregateId", event.publisherAggregateId());
        out.put("publisherAggregateVersion", event.publisherAggregateVersion()); out.put("published", event.published());
        if (eventService != null && eventService.getEventForReplay(event.eventId()) instanceof UpdateQuestionEvent update) {
            out.put("payload", Map.of("title", update.getTitle(), "content", update.getContent()));
        }
        return out;
    }

    private static Map<String, Object> failure(Throwable failure) {
        if (failure == null) return Map.of();
        Throwable root = root(failure);
        return Map.of("class", root.getClass().getName(), "message", String.valueOf(root.getMessage()));
    }

    private static boolean sameTournamentBusinessProjection(Map<String, Object> left, Map<String, Object> right) {
        return left.get("numberOfQuestions").equals(right.get("numberOfQuestions"))
                && left.get("startTime").equals(right.get("startTime"))
                && left.get("endTime").equals(right.get("endTime"))
                && left.get("topicIds").equals(right.get("topicIds"));
    }

    private static boolean checkpoint(String step, boolean explicit, boolean implicit, List<Map<String, Object>> values) {
        return values.stream().anyMatch(value -> step.equals(value.get("sourceStep"))
                && Boolean.valueOf(explicit).equals(value.get("explicitCompensationPending"))
                && Boolean.valueOf(implicit).equals(value.get("implicitRollbackPending")));
    }

    private static boolean recovery(String step, boolean explicit, boolean implicit, List<Map<String, Object>> values) {
        return values.stream().anyMatch(value -> step.equals(value.get("sourceStep"))
                && Boolean.valueOf(explicit).equals(value.get("explicitCompensationExecuted"))
                && Boolean.valueOf(implicit).equals(value.get("implicitRollbackExecuted")));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) { return (Map<String, Object>) value; }
    private static Throwable root(Throwable value) {
        Throwable current = value;
        while (current != null && current.getCause() != null && (current instanceof java.util.concurrent.CompletionException || current instanceof java.util.concurrent.ExecutionException)) current = current.getCause();
        return current;
    }

    private record Setup(int courseId, int creatorId, int topic1Id, int topic2Id, int topic3Id,
                         Integer tournamentId, Integer quizId, LocalDateTime now) {}

    private record Delivery(boolean success, Integer subscriberId, String failureClass, String failureMessage, String failureReason,
                            List<Map<String, Object>> dynamicEvidence) {
        Map<String, Object> asMap() {
            Map<String, Object> out = new LinkedHashMap<>(); out.put("success", success); out.put("subscriberId", subscriberId);
            out.put("failureClass", failureClass); out.put("failureMessage", failureMessage); out.put("failureReason", failureReason);
            out.put("dynamicEvidence", dynamicEvidence); return out;
        }
    }

    private static final class RecordingRecorder implements DynamicEvidenceRecorder {
        private final List<DynamicEvidenceEvent> events = new ArrayList<>();
        public boolean isEnabled() { return true; }
        public synchronized void record(DynamicEvidenceEvent event) { events.add(event); }
        public void close() {}
        synchronized void clear() { events.clear(); }
        @SuppressWarnings("unchecked")
        synchronized List<Map<String, Object>> drain() {
            List<Map<String, Object>> out = events.stream().map(e -> (Map<String, Object>) JSON.convertValue(e, Map.class)).toList();
            events.clear(); return out;
        }
    }
}
