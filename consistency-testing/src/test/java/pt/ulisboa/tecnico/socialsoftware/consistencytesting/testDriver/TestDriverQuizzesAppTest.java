package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantWithinMaxTournamentsFunctionalitySagas.MAX_TOURNAMENTS_PER_USER;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Oracle;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestCase;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testsupport.InitialState;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testsupport.QuizzesTestFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantWithinMaxTournamentsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.MoveParticipantBetweenTournamentsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestDriverQuizzesAppTest {

    private static final Logger log = LoggerFactory.getLogger(TestDriverQuizzesAppTest.class);

    private static final int ITERATIONS = 20;

    private @Nullable TestDriver driver;
    private @Nullable Oracle oracle;
    private @Nullable QuizzesTestFactory factory;

    @BeforeAll
    void setupAll() {
        Path reportsDirectory = Path.of("target/test-driver-reports");

        driver = new TestDriver(QuizzesSimulator.class, List.of(), reportsDirectory)
                .setIterations(ITERATIONS);
        oracle = driver.getOracle();

        driver.init();

        factory = new QuizzesTestFactory(
                oracle.getBean(SagaUnitOfWorkService.class),
                oracle.getBean(ExecutionFunctionalities.class),
                oracle.getBean(UserFunctionalities.class),
                oracle.getBean(TopicFunctionalities.class),
                oracle.getBean(QuestionFunctionalities.class),
                oracle.getBean(TournamentFunctionalities.class));
    }

    @AfterAll
    void tearDownAll() {
        driver.shutdown();
    }

    // TODO When a proper anomaly analyzer is implemented, tests that prove the
    // * tool finds anomalies autonomously should be added.

    @Test
    void driverExploresVariedInterleavings() {
        List<TestResult> results = driver.exploreTestCase(this::intersectingTournamentFunctionalitiesTestCase);

        assertEquals(ITERATIONS, results.size(), "driver should run the full iteration budget");
        assertTrue(results.stream().noneMatch(result -> result.schedule().isEmpty()),
                "every run should execute at least one step");

        long distinctSchedules = results.stream().map(TestResult::schedule).distinct().count();
        long distinctReadsFrom = results.stream().map(TestResult::readsFromRelations).distinct().count();
        assertTrue(distinctSchedules >= 2 || distinctReadsFrom >= 2,
                "randomized exploration should realize more than one interleaving, "
                        + "got " + distinctSchedules + " distinct schedules and "
                        + distinctReadsFrom + " distinct reads-from sets");
    }

    /**
     * Sets up a scenario with two functionalities that touch the same tournament
     * aggregate: one adds a participant, the other updates the tournament's topics.
     */
    private TestCase.Builder intersectingTournamentFunctionalitiesTestCase() {
        SagaUnitOfWorkService sagaUnitOfWorkService = oracle.getBean(SagaUnitOfWorkService.class);
        CommandGateway gateway = oracle.getBean(CommandGateway.class);

        InitialState initialState = factory.setupInitialState();

        AddParticipantFunctionalitySagas addParticipantSaga = factory.createAddParticipantFunctionality(
                sagaUnitOfWorkService,
                initialState.tournamentDto().getAggregateId(),
                initialState.courseExecutionDto().getAggregateId(),
                initialState.userDto().getAggregateId(),
                gateway);

        UpdateTournamentFunctionalitySagas updateTournamentSaga = factory.createUpdateTournamentFunctionality(
                sagaUnitOfWorkService,
                initialState.tournamentDto(),
                Set.of(initialState.topicDto().getAggregateId()),
                gateway);

        return new TestCase.Builder()
                .addFunctionality(
                        FunctionalityId.forSagaFunctionality("AddParticipantSaga"),
                        addParticipantSaga)
                .addFunctionality(
                        FunctionalityId.forSagaFunctionality("UpdateTournamentSaga"),
                        updateTournamentSaga);
    }

    /**
     * Reads the current tournament state and reports whether {@code userId} is a
     * participant. A tournament that can no longer be read (e.g. it was deleted)
     * holds no participants.
     */
    private boolean participantIsPresent(Integer tournamentId, Integer userId) {
        TournamentFunctionalities tournamentFunctionalities = oracle.getBean(TournamentFunctionalities.class);
        try {
            TournamentDto tournament = tournamentFunctionalities.findTournament(tournamentId);
            return tournament != null && tournament.findParticipant(userId) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Whether the tournament exists and is still readable, i.e. it was not deleted.
     */
    private boolean tournamentExists(Integer tournamentId) {
        TournamentFunctionalities tournamentFunctionalities = oracle.getBean(TournamentFunctionalities.class);
        try {
            return tournamentFunctionalities.findTournament(tournamentId) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // === WRITE SKEW ON THE MAX-TOURNAMENTS-PER-USER QUOTA ===

    private static final FunctionalityId JOIN_TOURNAMENT_1_FUNC_ID = FunctionalityId
            .forSagaFunctionality("joinTournament1Saga");

    private static final FunctionalityId JOIN_TOURNAMENT_2_FUNC_ID = FunctionalityId
            .forSagaFunctionality("joinTournament2Saga");

    /**
     * Ids captured by the most recent {@link #maxTournamentsQuotaTestCase()} build.
     */
    private @Nullable Integer quotaTournament1Id, quotaTournament2Id, quotaJoinerId;

    /**
     * Sets up the max-tournaments-per-user quota scenario WITHOUT
     * inter-dependencies:
     * two {@code AddParticipantWithinMaxTournamentsFunctionalitySagas} that add the
     * same user to two different tournaments (T1, T2) of the same course execution.
     * <p>
     * Ids of the two tournaments
     * ({@link #quotaTournament1Id}, {@link #quotaTournament2Id})
     * and the joiner ({@link #quotaJoinerId}) are captured in
     * this test class's fields to facilitate the final-data checks.
     */
    private TestCase.Builder maxTournamentsQuotaTestCase() {
        SagaUnitOfWorkService sagaUnitOfWorkService = oracle.getBean(SagaUnitOfWorkService.class);
        CommandGateway gateway = oracle.getBean(CommandGateway.class);

        InitialState initialState = factory.setupInitialState();
        Integer executionId = initialState.courseExecutionDto().getAggregateId();
        Integer topicId = initialState.topicDto().getAggregateId();
        Integer creatorId = initialState.userDto().getAggregateId();
        Integer tournament1 = initialState.tournamentDto().getAggregateId();

        // A second tournament in the SAME course execution: the quota is counted per
        // course execution, so both tournaments must fall inside the same one.
        Integer tournament2 = factory.createTournament(
                QuizzesTestFactory.TIME_1, QuizzesTestFactory.TIME_3, 1,
                creatorId, executionId, List.of(topicId)).getAggregateId();

        // The user who will try to join both tournaments concurrently.
        Integer joiner = factory.createStudentInExecution(executionId,
                QuizzesTestFactory.USER_NAME_2, QuizzesTestFactory.USER_USERNAME_2).getAggregateId();

        this.quotaTournament1Id = tournament1;
        this.quotaTournament2Id = tournament2;
        this.quotaJoinerId = joiner;

        AddParticipantWithinMaxTournamentsFunctionalitySagas joinTournament1 = factory
                .createAddParticipantWithinMaxTournamentsFunctionality(
                        sagaUnitOfWorkService, tournament1, executionId, joiner, gateway);

        AddParticipantWithinMaxTournamentsFunctionalitySagas joinTournament2 = factory
                .createAddParticipantWithinMaxTournamentsFunctionality(
                        sagaUnitOfWorkService, tournament2, executionId, joiner, gateway);

        return new TestCase.Builder()
                .addFunctionality(JOIN_TOURNAMENT_1_FUNC_ID, joinTournament1)
                .addFunctionality(JOIN_TOURNAMENT_2_FUNC_ID, joinTournament2);
    }

    /**
     * Deterministically forces the write-skew window on the max-tournaments quota:
     * both joins count the user's tournaments before either of them adds the user,
     * so both counts come back under the quota and both adds go through.
     * The oracle flags the resulting state through the {@code max-tournaments}
     * inter-invariant.
     * <p>
     * Neither tournament aggregate can see the violation on its own - the quota
     * spans both - and no serial execution produces it: run one after the other,
     * the second join's count would already include the first one's participant and
     * would simply give up and not add the user.
     */
    @Test
    void forcedConcurrentJoinsBreakMaxTournamentsInterInvariant() {
        StepId join1Count = StepId.forFunctionalityStep(JOIN_TOURNAMENT_1_FUNC_ID, "countUserTournamentsStep");
        StepId join1Add = StepId.forFunctionalityStep(JOIN_TOURNAMENT_1_FUNC_ID, "addParticipantStep");
        StepId join2Count = StepId.forFunctionalityStep(JOIN_TOURNAMENT_2_FUNC_ID, "countUserTournamentsStep");
        StepId join2Add = StepId.forFunctionalityStep(JOIN_TOURNAMENT_2_FUNC_ID, "addParticipantStep");

        Supplier<TestCase> setup = () -> maxTournamentsQuotaTestCase()
                // both quota checks before both adds -> the write-skew window opens
                .addInterDependency(join1Add, join2Count)
                .addInterDependency(join2Add, join1Count)
                .build();

        AtomicReference<Integer> tournamentCountRef = new AtomicReference<>();

        TestResult result = oracle.runTest(setup, res -> {
            int count = (participantIsPresent(quotaTournament1Id, quotaJoinerId) ? 1 : 0)
                    + (participantIsPresent(quotaTournament2Id, quotaJoinerId) ? 1 : 0);
            tournamentCountRef.set(count);

            List<StepId> schedule = res.schedule();
            log.info("forced quota write-skew: user in {} tournament(s) (quota={}), statuses={}",
                    count, MAX_TOURNAMENTS_PER_USER, res.statuses());
            assertTrue(schedule.indexOf(join1Add) > schedule.indexOf(join2Count)
                    && schedule.indexOf(join2Add) > schedule.indexOf(join1Count),
                    "both quota checks must precede both adds for the write-skew window to open");
        });

        assertTrue(result.statuses().contains(TestStatus.INTER_INVARIANT_VIOLATION),
                "the oracle should autonomously flag the max-tournaments inter-invariant violation");
        assertTrue(tournamentCountRef.get() > MAX_TOURNAMENTS_PER_USER);
    }

    /**
     * Lets the {@link TestDriver} freely explore the quota scenario (varied
     * inter-dependencies and scheduler seeds) and asserts it realizes BOTH
     * outcomes: some interleavings open the write-skew window and break the
     * max-tournaments inter-invariant, others serialize the two joins and respect
     * it. This proves the driver actually explores the scenario rather than always
     * hitting the same interleaving.
     * <p>
     * A run only counts as "respecting" if it completed cleanly - both joins
     * committed, no step threw, and no status was raised. Otherwise a run that
     * crashed before the second add would masquerade as a serialized one and the
     * test would pass for the wrong reason.
     */
    @Test
    void driverExploresQuotaScenarioFindingBreakingAndNonBreakingRuns() {
        List<TestResult> results = driver.exploreTestCase(this::maxTournamentsQuotaTestCase);

        assertEquals(ITERATIONS, results.size(), "driver should run the full iteration budget");

        long breaking = results.stream()
                .filter(result -> result.statuses().contains(TestStatus.INTER_INVARIANT_VIOLATION))
                .count();
        long cleanlyRespecting = results.stream()
                .filter(TestDriverQuizzesAppTest::isCleanQuotaRespectingRun)
                .count();
        log.info("quota exploration over {} run(s): {} broke the max-tournaments inter-invariant, "
                + "{} completed cleanly while respecting it",
                results.size(), breaking, cleanlyRespecting);

        assertTrue(breaking > 0,
                "driver should explore at least one interleaving that breaks the quota inter-invariant");
        assertTrue(cleanlyRespecting > 0,
                "driver should explore at least one interleaving where both joins complete cleanly and "
                        + "the quota inter-invariant still holds (i.e. the two joins were serialized)");
    }

    /**
     * A run in which both joins ran to completion, nothing failed, and no status
     * (including no inter-invariant violation) was raised.
     */
    private static boolean isCleanQuotaRespectingRun(TestResult result) {
        return result.exceptions().isEmpty()
                && result.statuses().isEmpty()
                && result.schedule().containsAll(List.of(
                        StepId.forCommitStep(JOIN_TOURNAMENT_1_FUNC_ID),
                        StepId.forCommitStep(JOIN_TOURNAMENT_2_FUNC_ID)));
    }

    // === A COMPENSATION THAT BECOMES IMPOSSIBLE ===

    private static final FunctionalityId MOVE_PARTICIPANT_FUNC_ID = FunctionalityId
            .forSagaFunctionality("MoveParticipantSaga");

    private static final FunctionalityId REMOVE_SOURCE_TOURNAMENT_FUNC_ID = FunctionalityId
            .forSagaFunctionality("RemoveSourceTournamentSaga");

    /**
     * Ids captured by the most recent {@link #impossibleCompensationTestCase()}
     * build.
     */
    private @Nullable Integer sourceTournamentId, targetTournamentId, movedUserId;

    /**
     * Sets up the impossible-compensation scenario WITHOUT inter-dependencies: a
     * user enrolled in a source tournament is moved to a target tournament that has
     * ALREADY STARTED (so the move is bound to fail and compensate), racing a
     * deletion of the source tournament (which is only legal while that tournament
     * has no participants, and could be true only temporarily while the move is
     * in progress and before it compensates).
     */
    private TestCase.Builder impossibleCompensationTestCase() {
        SagaUnitOfWorkService sagaUnitOfWorkService = oracle.getBean(SagaUnitOfWorkService.class);
        CommandGateway gateway = oracle.getBean(CommandGateway.class);

        InitialState initialState = factory.setupInitialState();
        Integer executionId = initialState.courseExecutionDto().getAggregateId();
        Integer topicId = initialState.topicDto().getAggregateId();
        Integer creatorId = initialState.userDto().getAggregateId();
        Integer sourceTournamentId = initialState.tournamentDto().getAggregateId();

        // The target tournament is already running, so enrolling anyone in it is
        // rejected by ENROLL_UNTIL_START_TIME:
        // the move always fails there and compensates.
        Integer targetTournamentId = factory.createStartedTournament(creatorId, executionId, List.of(topicId))
                .getAggregateId();

        // The user being moved: is enrolled in the source tournament up front,
        // which is also what makes the source tournament undeletable until they leave.
        Integer userId = factory.createStudentInExecution(executionId,
                QuizzesTestFactory.USER_NAME_2, QuizzesTestFactory.USER_USERNAME_2).getAggregateId();
        factory.addParticipant(sourceTournamentId, executionId, userId);

        this.sourceTournamentId = sourceTournamentId;
        this.targetTournamentId = targetTournamentId;
        this.movedUserId = userId;

        MoveParticipantBetweenTournamentsFunctionalitySagas move = factory
                .createMoveParticipantBetweenTournamentsFunctionality(
                        sagaUnitOfWorkService, sourceTournamentId, targetTournamentId, executionId, userId, gateway);

        RemoveTournamentFunctionalitySagas removeSource = factory
                .createRemoveTournamentFunctionality(sagaUnitOfWorkService, sourceTournamentId, gateway);

        return new TestCase.Builder()
                .addFunctionality(MOVE_PARTICIPANT_FUNC_ID, move)
                .addFunctionality(REMOVE_SOURCE_TOURNAMENT_FUNC_ID, removeSource);
    }

    /**
     * Deterministically forces the interleaving in which the move's compensation
     * becomes impossible, and asserts the user ends up in NEITHER tournament.
     * <p>
     * {@code MoveParticipantSaga} takes the user out of the source tournament and
     * then fails to enrol them in the target one (it has already started, so
     * ENROLL_UNTIL_START_TIME rejects the add). Its compensation puts the user back
     * into the source tournament, which is always legal on its own.
     * {@code RemoveSourceTournamentSaga} is forced to read the source tournament in
     * the window between the departure and the compensation: it sees an empty
     * tournament, so the DELETE invariant
     * ({@code state == DELETED => participants.empty}) lets the deletion through.
     * The compensation then tries to re-add the user to a deleted tournament and is
     * rejected by that same invariant.
     * <p>
     * The outcome is not producible by any serial execution: alone, the delete
     * would have been refused (the participant was still there) and the
     * compensation would have succeeded. Here the saga neither committed nor rolled
     * back - the user is gone from both tournaments and the source tournament is
     * deleted.
     */
    @Test
    void forcedConcurrentDeleteMakesMoveCompensationImpossible() {
        StepId leaveSourceStep = StepId.forFunctionalityStep(MOVE_PARTICIPANT_FUNC_ID, "leaveSourceTournamentStep");
        StepId addToTargetStep = StepId.forFunctionalityStep(MOVE_PARTICIPANT_FUNC_ID, "addToTargetTournamentStep");
        StepId deleteReadStep = StepId.forFunctionalityStep(REMOVE_SOURCE_TOURNAMENT_FUNC_ID, "getTournamentStep");
        StepId deleteStep = StepId.forFunctionalityStep(REMOVE_SOURCE_TOURNAMENT_FUNC_ID, "removeTournamentStep");

        Supplier<TestCase> setup = () -> impossibleCompensationTestCase()
                // the deleter reads the source tournament only AFTER the user has left it,
                // so the tournament looks empty and the deletion looks legal ...
                .addInterDependency(deleteReadStep, leaveSourceStep)
                // ... and the deletion lands BEFORE the move fails, so the compensation finds
                // a tournament that no longer exists
                .addInterDependency(addToTargetStep, deleteStep)
                .build();

        AtomicReference<Boolean> inSourceRef = new AtomicReference<>();
        AtomicReference<Boolean> inTargetRef = new AtomicReference<>();

        TestResult result = oracle.runTest(setup, res -> {
            boolean inSource = participantIsPresent(sourceTournamentId, movedUserId);
            boolean inTarget = participantIsPresent(targetTournamentId, movedUserId);
            inSourceRef.set(inSource);
            inTargetRef.set(inTarget);

            log.info("forced impossible compensation: user in source = {}, user in target = {}, "
                    + "source tournament still exists = {}, statuses={}, exceptions={}",
                    inSource, inTarget, tournamentExists(sourceTournamentId),
                    res.statuses(), res.exceptions().keySet());

            List<StepId> schedule = res.schedule();
            assertTrue(schedule.indexOf(deleteReadStep) > schedule.indexOf(leaveSourceStep),
                    "the deleter must read the source tournament after the user has left it");

            // The delete must really have read the version the move published, not the
            // initial one - otherwise it would have seen the participant and refused.
            boolean readTheDepartedVersion = res.readsFromRelations().stream()
                    .filter(r -> QuizzesTestFactory.TOURNAMENT_AGGREGATE_TYPE.equals(r.aggregateType()))
                    .filter(r -> r.reader().equals(deleteReadStep))
                    .anyMatch(r -> r.writer().equals(leaveSourceStep));
            assertTrue(readTheDepartedVersion,
                    "the deleter should read the source-tournament version written by the departure");

            // ... and the move must have failed on the target tournament, which is what
            // sends it down the compensation path in the first place. Note the move's
            // AbortStep is NOT in the schedule: the compensation itself blows up, and a
            // failing compensation is a critical failure that halts the run.
            assertTrue(res.exceptions().containsKey(addToTargetStep),
                    "the move should have failed to enrol the user in the already-started target "
                            + "tournament, but that failure was not registered in the verified "
                            + "exceptions=" + res.exceptions().keySet());
        });

        assertTrue(result.statuses().contains(TestStatus.CRITICAL_STEP_FAILURE),
                "the oracle should flag the failed compensation as a critical step failure");
        assertFalse(inSourceRef.get(),
                "the compensation should not be able to put the user back into the source tournament: "
                        + "it was deleted while the move was in flight");
        assertFalse(inTargetRef.get(),
                "the move should have never enrolled the user in the target tournament (it had already started)");

        // TODO An AnomalyAnalyzer fed with the full effectSequence
        // (every read and every write, in order, with the aggregate id they touched)
        // could flag this run WITHOUT the test having to know the scenario.
        // Two patterns are visible purely in the effect sequence:
        // (1) a compensating write that is REJECTED on an aggregate that a foreign
        // functionality wrote after the effect being compensated - i.e. the undo was
        // invalidated by an interposed write. That is the signature of a saga that can
        // neither commit nor roll back, and it is what makes this different from a saga
        // that simply breaks its own compensation on its own (no interposed writer).
        // (2) the deleting saga read-from an effect (the departure) that was later
        // compensated - a dirty read - and COMMITTED a decision (the deletion) based on
        // it.
    }

    /**
     * Lets the driver explore the {@link #impossibleCompensationTestCase()}
     * scenario freely and asserts BOTH outcomes occur: some interleavings slip the
     * deletion into the window where the source tournament looks empty and strand
     * the compensation, others let the move compensate cleanly (and the deletion is
     * then correctly refused, since the participant is back). The anomaly is real
     * but not guaranteed to hit - which is the point of the exploration.
     */
    @Test
    void driverExploresImpossibleCompensationFindingStrandedAndRecoveredRuns() {
        List<TestResult> results = driver.exploreTestCase(this::impossibleCompensationTestCase);
        assertEquals(ITERATIONS, results.size(), "driver should run the full iteration budget");

        long stranded = results.stream()
                .filter(result -> result.statuses().contains(TestStatus.CRITICAL_STEP_FAILURE))
                .count();
        long recovered = results.size() - stranded;
        log.info("impossible-compensation exploration over {} run(s): "
                + "{} stranded the move's compensation, {} let it compensate",
                results.size(), stranded, recovered);

        assertTrue(stranded > 0,
                "driver should explore at least one interleaving where the source tournament is deleted "
                        + "in the window where it looks empty, making the move's compensation impossible");
        assertTrue(recovered > 0,
                "driver should also explore interleavings where the compensation succeeds, showing the "
                        + "anomaly is interleaving-dependent and not always reachable");
    }
}
