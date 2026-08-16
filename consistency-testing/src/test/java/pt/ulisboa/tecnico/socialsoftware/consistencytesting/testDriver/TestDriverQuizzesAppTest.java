package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantWithinMaxTournamentsFunctionalitySagas.MAX_TOURNAMENTS_PER_USER;

import java.nio.file.Path;
import java.util.ArrayList;
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

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Anomaly;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.AnomalyType;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Oracle;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepEffect;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepKind;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestCase;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;
import pt.ulisboa.tecnico.socialsoftware.quizzes.oracle.InitialState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.oracle.QuizzesTestFactory;
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
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.GenerateTournamentSummaryFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.LeaveTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.MoveParticipantBetweenTournamentsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.UpdateStudentNameFunctionalitySagas;
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

    // =============================================================================================
    // WRITE SKEW ON THE MAX-TOURNAMENTS-PER-USER QUOTA
    // =============================================================================================

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
                QuizzesTestFactory.time1(), QuizzesTestFactory.time3(), 1,
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

        // The AnomalyAnalyzer must reach the same verdict INDEPENDENTLY of the
        // inter-invariant: purely from the effect sequence, it should see the two
        // joins as a write skew (mutual rw anti-dependencies on the two
        // tournaments, no wr edge, both committed).
        assertTrue(result.statuses().contains(TestStatus.ISOLATION_ANOMALY),
                "the anomaly analyzer should flag the run, statuses=" + result.statuses());
        assertTrue(result.anomalies().stream()
                .anyMatch(anomaly -> anomaly instanceof Anomaly.WriteSkew writeSkew
                        && Set.of(writeSkew.functionalityA(), writeSkew.functionalityB())
                                .equals(Set.of(JOIN_TOURNAMENT_1_FUNC_ID, JOIN_TOURNAMENT_2_FUNC_ID))),
                "the analyzer should report a WRITE_SKEW between the two joins, got: " + result.anomalies());
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

        // every run the inter-invariant flagged (final data over the quota) must also
        // have been flagged by the AnomalyAnalyzer (write skew visible in the effect
        // sequence) — the analyzer needs no app-specific invariant to see it.
        assertTrue(results.stream()
                .filter(result -> result.statuses().contains(TestStatus.INTER_INVARIANT_VIOLATION))
                .allMatch(result -> result.anomalies().stream()
                        .anyMatch(anomaly -> anomaly.type() == AnomalyType.WRITE_SKEW)),
                "every quota-breaking run should also carry a WRITE_SKEW anomaly");
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

    // =============================================================================================
    // A COMPENSATION THAT BECOMES IMPOSSIBLE
    // =============================================================================================

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

        // The AnomalyAnalyzer flags this run WITHOUT the test having to know the
        // scenario: the deleter read the departure write (a write of the move,
        // which later entered its compensation path) and committed the deletion
        // based on it — a dirty read, visible purely in the effect sequence.
        assertTrue(result.statuses().contains(TestStatus.ISOLATION_ANOMALY),
                "the anomaly analyzer should flag the run, statuses=" + result.statuses());
        assertTrue(result.anomalies().stream()
                .anyMatch(anomaly -> anomaly instanceof Anomaly.DirtyRead dirtyRead
                        && dirtyRead.reader().equals(
                                StepId.forFunctionalityStep(REMOVE_SOURCE_TOURNAMENT_FUNC_ID, "getTournamentStep"))
                        && dirtyRead.doomedWriter().equals(
                                StepId.forFunctionalityStep(MOVE_PARTICIPANT_FUNC_ID, "leaveSourceTournamentStep"))
                        && QuizzesTestFactory.TOURNAMENT_AGGREGATE_TYPE.equals(dirtyRead.aggregateType())),
                "the analyzer should report the deleter's DIRTY_READ of the departure write, got: "
                        + result.anomalies());
    }

    /**
     * The data the impossible-compensation scenario left behind, read while the run
     * that produced it is still in the database.
     */
    private record CompensationRunOutcome(boolean userInSource, boolean userInTarget, boolean sourceExists) {
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
        StepId addToTargetStep = StepId.forFunctionalityStep(MOVE_PARTICIPANT_FUNC_ID, "addToTargetTournamentStep");
        StepId moveCompensationStep = StepId.forCompensationStep(
                MOVE_PARTICIPANT_FUNC_ID, "leaveSourceTournamentStep");
        StepId deleteStep = StepId.forFunctionalityStep(REMOVE_SOURCE_TOURNAMENT_FUNC_ID, "removeTournamentStep");

        List<CompensationRunOutcome> outcomes = new ArrayList<>();
        List<TestResult> results = driver.exploreTestCase(
                this::impossibleCompensationTestCase,
                result -> outcomes.add(new CompensationRunOutcome(
                        participantIsPresent(sourceTournamentId, movedUserId),
                        participantIsPresent(targetTournamentId, movedUserId),
                        tournamentExists(sourceTournamentId))));

        assertEquals(ITERATIONS, results.size(), "driver should run the full iteration budget");
        assertEquals(results.size(), outcomes.size(), "every run's final state should have been captured");

        long stranded = 0;
        long recovered = 0;
        long recoveredWithNonRepeatableRead = 0;
        long resolutionFailed = 0;
        for (int run = 0; run < results.size(); run++) {
            TestResult result = results.get(run);
            CompensationRunOutcome outcome = outcomes.get(run);

            // A run the driver poisoned with a constraint on a step this run never
            // created: the scenario did not play out. Tolerated (threshold bounded below),
            // and it excludes itself from the stranded/recovered classification.
            if (result.statuses().contains(TestStatus.INTERDEPENDENCY_RESOLUTION_FAILED)) {
                resolutionFailed++;
                continue;
            }

            boolean strandedRun = result.statuses().contains(TestStatus.CRITICAL_STEP_FAILURE)
                    && result.exceptions().containsKey(addToTargetStep)
                    && result.exceptions().containsKey(moveCompensationStep)
                    && result.anomalies().stream().anyMatch(anomaly -> anomaly.type() == AnomalyType.DIRTY_READ)
                    && !outcome.userInSource() && !outcome.userInTarget() && !outcome.sourceExists();

            boolean recoveredRun = !result.statuses().contains(TestStatus.CRITICAL_STEP_FAILURE)
                    && result.anomalies().stream().noneMatch(anomaly -> anomaly.type() == AnomalyType.DIRTY_READ)
                    && !result.exceptions().containsKey(moveCompensationStep)
                    && result.exceptions().containsKey(deleteStep)
                    && outcome.userInSource() && !outcome.userInTarget() && outcome.sourceExists();

            boolean recoveredWithNrr = recoveredRun
                    && result.anomalies().stream()
                            .anyMatch(anomaly -> anomaly.type() == AnomalyType.NON_REPEATABLE_READ);

            stranded += strandedRun ? 1 : 0;
            recovered += recoveredRun ? 1 : 0;
            recoveredWithNonRepeatableRead += recoveredWithNrr ? 1 : 0;

            assertFalse(strandedRun && recoveredRun,
                    "a run cannot both strand the compensation and let it succeed.");

            assertTrue(strandedRun || recoveredRun,
                    "a run that played out the scenario should (exclusively) either strand the compensation or let it "
                            + "succeed, but run " + run + " did neither: statuses=" + result.statuses()
                            + ", exceptions=" + result.exceptions().keySet()
                            + ", anomalies=" + result.anomalies()
                            + ", finalState=" + outcome);
        }

        log.info("impossible-compensation exploration over {} run(s): "
                + "{} stranded the move's compensation, {} let it compensate "
                + "(being that {} of those carried a non-repeatable read), "
                + "{} did not play out (interdependency resolution failed)",
                results.size(), stranded, recovered, recoveredWithNonRepeatableRead, resolutionFailed);

        assertTrue(stranded > 0,
                "driver should explore at least one interleaving where the source tournament is deleted "
                        + "in the window where it looks empty, making the move's compensation impossible");
        assertTrue(recovered > 0,
                "driver should also explore interleavings where the compensation succeeds, showing the "
                        + "anomaly is interleaving-dependent and not always reachable");
        assertTrue(recoveredWithNonRepeatableRead > 0 && recoveredWithNonRepeatableRead < recovered / 2,
                "driver should explore at least one interleaving where the compensation succeeds - so no "
                        + "dirty read and no failed compensation - yet the move's departure still "
                        + "happens between the deleter's two reads of the source tournament,"
                        + "leaving a NON_REPEATABLE_READ: a milder isolation anomaly that "
                        + "surfaces even when the run recovers, "
                        + "but not every recovered run should carry it, just a few");
        assertTrue(resolutionFailed < results.size() / 4,
                "most of the exploration budget should have played out the scenario; too many runs "
                        + "dissolved into unsatisfiable inter-dependencies (" + resolutionFailed
                        + " of " + results.size() + ")");
    }

    // =============================================================================================
    // A NON-REPEATABLE READ IN A SUMMARY GENERATION
    // =============================================================================================

    private static final FunctionalityId GENERATE_SUMMARY_FUNC_ID = FunctionalityId
            .forSagaFunctionality("GenerateSummarySaga");

    private static final FunctionalityId LEAVE_TOURNAMENT_FUNC_ID = FunctionalityId
            .forSagaFunctionality("LeaveTournamentSaga");

    /**
     * The summary functionality built by the most recent
     * {@link #summaryNonRepeatableReadTestCase()} build.
     */
    private @Nullable GenerateTournamentSummaryFunctionalitySagas summarySaga;

    /**
     * The participant id captured by the most recent
     * {@link #summaryNonRepeatableReadTestCase()} build,
     * whose departure races the {@link #summarySaga}.
     */
    private @Nullable Integer summaryParticipantId;

    /**
     * Sets up the summary non-repeatable-read scenario WITHOUT
     * inter-dependencies: a {@code GenerateTournamentSummarySaga} racing a
     * {@code LeaveTournamentSaga} of the tournament's only participant.
     * <p>
     * The summary saga loads the tournament ONCE and caches the DTO — but its
     * participant-details step re-reads the tournament unknowingly, once per
     * participant, inside the participant-lookup command (participants live in
     * the Tournament aggregate). When the departure lands between the
     * deliberate load and the hidden one, the lookup fails for a participant
     * the saga itself validated a moment earlier and no report is produced.
     */
    private TestCase.Builder summaryNonRepeatableReadTestCase() {
        SagaUnitOfWorkService sagaUnitOfWorkService = oracle.getBean(SagaUnitOfWorkService.class);
        CommandGateway gateway = oracle.getBean(CommandGateway.class);

        InitialState initialState = factory.setupInitialState();
        Integer executionId = initialState.courseExecutionDto().getAggregateId();
        Integer tournamentId = initialState.tournamentDto().getAggregateId();

        // The tournament's only participant — enrolled up front, so the summary's
        // first load lists them; their concurrent departure is the problematic write.
        Integer participantId = factory.createStudentInExecution(executionId,
                QuizzesTestFactory.USER_NAME_2, QuizzesTestFactory.USER_USERNAME_2).getAggregateId();
        factory.addParticipant(tournamentId, executionId, participantId);
        this.summaryParticipantId = participantId;

        GenerateTournamentSummaryFunctionalitySagas summary = factory
                .createGenerateTournamentSummaryFunctionality(sagaUnitOfWorkService, tournamentId, gateway);
        this.summarySaga = summary;

        LeaveTournamentFunctionalitySagas leave = factory.createLeaveTournamentFunctionality(
                sagaUnitOfWorkService, tournamentId, participantId, gateway);

        return new TestCase.Builder()
                .addFunctionality(GENERATE_SUMMARY_FUNC_ID, summary)
                .addFunctionality(LEAVE_TOURNAMENT_FUNC_ID, leave);
    }

    /**
     * Deterministically forces the participant's departure between the
     * summary's deliberate tournament load and its hidden one (the
     * per-participant lookup), and asserts both the observable harm and the
     * autonomous detection:
     * <ul>
     * <li>harm — the summary generation blows up on a participant it itself
     * validated a moment earlier: the cached list (first read) still shows the
     * user, so the saga asks for their detail block, but the lookup (second
     * read, post-departure) fails with TOURNAMENT_PARTICIPANT_NOT_FOUND and no
     * report is produced;</li>
     * <li>detection — the AnomalyAnalyzer, knowing nothing about summaries,
     * reports a NON_REPEATABLE_READ: the summary functionality read the same
     * tournament twice and resolved the reads to different writers, the second
     * one being the foreign departure.</li>
     * </ul>
     */
    @Test
    void forcedLeaveBetweenSummaryReadsCausesNonRepeatableRead() {
        StepId summaryLoadStep = StepId.forFunctionalityStep(GENERATE_SUMMARY_FUNC_ID, "getTournamentStep");
        StepId summaryDetailsStep = StepId.forFunctionalityStep(GENERATE_SUMMARY_FUNC_ID,
                "fetchParticipantDetailsStep");
        StepId leaveWriteStep = StepId.forFunctionalityStep(LEAVE_TOURNAMENT_FUNC_ID, "leaveTournamentStep");

        Supplier<TestCase> setup = () -> summaryNonRepeatableReadTestCase()
                // the departure lands only AFTER the summary cached its tournament DTO ...
                .addInterDependency(leaveWriteStep, summaryLoadStep)
                // ... and the per-participant lookup (the hidden second read) runs only
                // AFTER the departure was written
                .addInterDependency(summaryDetailsStep, leaveWriteStep)
                .build();

        TestResult result = oracle.runTest(setup, res -> {
            boolean listedAsParticipant = summarySaga.getSummaryTournamentDto().getParticipants().stream()
                    .anyMatch(participant -> participant.getAggregateId().equals(summaryParticipantId));

            log.info("forced summary non-repeatable read: participant listed = {}, "
                    + "detail blocks fetched = {}, statuses={}, exceptions={}",
                    listedAsParticipant, summarySaga.getParticipantDetails().keySet(),
                    res.statuses(), res.exceptions().keySet());

            // the observable harm: the saga validated the user as a participant on
            // its first read, then crashed fetching that same user's detail block
            // on the hidden second read — no report was produced
            assertTrue(listedAsParticipant,
                    "the cached participant list should still show the user (loaded before the departure)");
            assertTrue(summarySaga.getParticipantDetails().isEmpty(),
                    "no detail block should have been produced: the lookup failed on the departed participant");
            assertTrue(res.exceptions().containsKey(summaryDetailsStep),
                    "the per-participant lookup should have failed for the departed participant, "
                            + "exceptions=" + res.exceptions().keySet());
        });

        assertTrue(result.statuses().contains(TestStatus.ISOLATION_ANOMALY),
                "the anomaly analyzer should flag the run, statuses=" + result.statuses());
        assertTrue(result.anomalies().stream()
                .anyMatch(anomaly -> anomaly instanceof Anomaly.NonRepeatableRead nonRepeatableRead
                        && nonRepeatableRead.functionality().equals(GENERATE_SUMMARY_FUNC_ID)
                        && nonRepeatableRead.firstRead().equals(summaryLoadStep)
                        && nonRepeatableRead.secondRead().equals(summaryDetailsStep)
                        && nonRepeatableRead.secondWriter().equals(leaveWriteStep)
                        && !nonRepeatableRead.secondWriter().equals(nonRepeatableRead.firstWriter())
                        && QuizzesTestFactory.TOURNAMENT_AGGREGATE_TYPE.equals(nonRepeatableRead.aggregateType())),
                "the analyzer should report the summary's NON_REPEATABLE_READ of the tournament, got: "
                        + result.anomalies());
    }

    /**
     * Lets the driver explore the summary scenario freely and asserts BOTH
     * outcomes occur: some interleavings slip the departure between the
     * summary's deliberate load and its hidden per-participant lookup
     * (non-repeatable read), others keep the two reads on the same version and
     * the summary completes with no anomaly flagged.
     */
    @Test
    void driverExploresSummaryScenarioFindingInconsistentAndConsistentSummaries() {
        List<TestResult> results = driver.exploreTestCase(this::summaryNonRepeatableReadTestCase);
        assertEquals(ITERATIONS, results.size(), "driver should run the full iteration budget");

        long inconsistent = results.stream()
                .filter(result -> result.anomalies().stream()
                        .anyMatch(anomaly -> anomaly.type() == AnomalyType.NON_REPEATABLE_READ))
                .count();
        long cleanlyConsistent = results.stream()
                .filter(result -> result.exceptions().isEmpty()
                        && result.statuses().isEmpty()
                        && result.schedule().containsAll(List.of(
                                StepId.forCommitStep(GENERATE_SUMMARY_FUNC_ID),
                                StepId.forCommitStep(LEAVE_TOURNAMENT_FUNC_ID))))
                .count();
        log.info("summary exploration over {} run(s): {} produced a non-repeatable read, "
                + "{} completed cleanly with a consistent summary",
                results.size(), inconsistent, cleanlyConsistent);

        assertTrue(inconsistent > 0,
                "driver should explore at least one interleaving where the departure lands between the "
                        + "summary's deliberate tournament load and its hidden per-participant lookup");
        assertTrue(cleanlyConsistent > 0,
                "driver should also explore interleavings where both functionalities complete cleanly "
                        + "and the summary reads a single consistent version");
    }

    // =============================================================================================
    // AN EVENT-HANDLER-INDUCED NON-REPEATABLE READ (DENORMALIZATION SYNC)
    // =============================================================================================

    private static final FunctionalityId RENAME_SUMMARY_FUNC_ID = FunctionalityId
            .forSagaFunctionality("RenameScenarioSummarySaga");

    private static final FunctionalityId RENAME_STUDENT_FUNC_ID = FunctionalityId
            .forSagaFunctionality("RenameStudentSaga");

    private static final String RENAMED_STUDENT_NAME = "RENAMED_STUDENT_NAME";

    /** Ids captured by the most recent {@link #studentRenameSyncTestCase()} build. */
    private @Nullable Integer renameTournamentId, renameStudentId;

    /**
     * Sets up the event-handler denormalization-sync scenario WITHOUT
     * inter-dependencies: a {@code GenerateTournamentSummarySaga} racing a
     * course-execution student rename.
     * <p>
     * The Tournament aggregate keeps a denormalized copy of each participant's
     * name. Renaming the student in the Execution emits an
     * {@code UpdateStudentNameEvent}; the Tournament subscribes and its
     * {@code UpdateStudentNameEventHandler} rewrites that cached name — a real
     * write on the Tournament aggregate, performed by an EVENT_HANDLER step
     * (a nested saga the oracle materializes and schedules on its own).
     * <p>
     * The summary reads the tournament twice (the deliberate load, then the
     * hidden per-participant lookup). When the handler's name-write lands between
     * those two reads, the summary observes two different versions of the same
     * tournament: a NON_REPEATABLE_READ whose interposing writer is an event
     * handler rather than a saga step. Unlike the {@code LeaveTournament}
     * scenario, the participant is only renamed (never removed), so the summary
     * never crashes — the harm is a silently inconsistent report, and the anomaly
     * is what surfaces it.
     */
    private TestCase.Builder studentRenameSyncTestCase() {
        SagaUnitOfWorkService sagaUnitOfWorkService = oracle.getBean(SagaUnitOfWorkService.class);
        CommandGateway gateway = oracle.getBean(CommandGateway.class);

        InitialState initialState = factory.setupInitialState();
        Integer executionId = initialState.courseExecutionDto().getAggregateId();
        Integer tournamentId = initialState.tournamentDto().getAggregateId();

        // The tournament's participant whose name will be synced. Enrolled up
        // front, so the summary's first load lists them and their (denormalized)
        // name; the concurrent rename is the interposing write.
        Integer studentId = factory.createStudentInExecution(executionId,
                QuizzesTestFactory.USER_NAME_2, QuizzesTestFactory.USER_USERNAME_2).getAggregateId();
        factory.addParticipant(tournamentId, executionId, studentId);

        this.renameTournamentId = tournamentId;
        this.renameStudentId = studentId;

        GenerateTournamentSummaryFunctionalitySagas summary = factory
                .createGenerateTournamentSummaryFunctionality(sagaUnitOfWorkService, tournamentId, gateway);

        UpdateStudentNameFunctionalitySagas rename = factory.createUpdateStudentNameFunctionality(
                sagaUnitOfWorkService, executionId, studentId, RENAMED_STUDENT_NAME, gateway);

        return new TestCase.Builder()
                .addFunctionality(RENAME_SUMMARY_FUNC_ID, summary)
                .addFunctionality(RENAME_STUDENT_FUNC_ID, rename);
    }

    /**
     * Lets the driver explore the denormalization-sync scenario freely and
     * asserts three things:
     * <ul>
     * <li>the event-handler path actually fires — some run materializes and runs
     * the tournament's name-sync handler, writing the tournament aggregate from
     * an EVENT_HANDLER step (this is the first oracle scenario to exercise event
     * handling end to end);</li>
     * <li>at least one run exhibits a NON_REPEATABLE_READ on the tournament whose
     * interposing writer is that event-handler write — i.e. the analyzer captures
     * an anomaly induced by an event handler, with no analyzer knowledge of the
     * scenario;</li>
     * <li>at least one run completes cleanly (both functionalities commit, no
     * anomaly), showing the hazard is interleaving-dependent.</li>
     * </ul>
     *
     * <p>
     * TODO complementary scenario for the OTHER event-handler path — the handler
     * as the anomaly's VICTIM (reader), i.e. a DIRTY_READ where an event handler
     * reads an aggregate a foreign functionality wrote and then compensated. The
     * analyzer already supports it (see {@code AnomalyAnalyzer#isApplicationEffect}
     * and its unit test {@code eventHandlerReadOfCompensatedWriteIsDirty}), but
     * staging it end to end needs a handler that reads a doomed aggregate write,
     * which no current handler does naturally — likely a small purpose-built
     * handler/functionality. Left as a separate, scoped follow-up.
     */
    @Test
    void driverExploresRenameSyncFindingStaleAndConsistentSummaries() {
        List<TestResult> results = driver.exploreTestCase(this::studentRenameSyncTestCase);
        assertEquals(ITERATIONS, results.size(), "driver should run the full iteration budget");

        boolean anyEventHandlerWrite = results.stream()
                .flatMap(result -> result.effectSequence().stream())
                .anyMatch(effect -> effect.stepKind() == StepKind.EVENT_HANDLER && effect.isWrite());
        assertTrue(anyEventHandlerWrite,
                "the tournament's UpdateStudentName event handler should have run and written the "
                        + "denormalized participant name — the event-handling path must actually fire");

        long staleSummaries = results.stream()
                .filter(TestDriverQuizzesAppTest::isEventHandlerInducedStaleSummary)
                .count();
        long cleanlyConsistent = results.stream()
                .filter(result -> result.exceptions().isEmpty()
                        && result.statuses().isEmpty()
                        && result.schedule().containsAll(List.of(
                                StepId.forCommitStep(RENAME_SUMMARY_FUNC_ID),
                                StepId.forCommitStep(RENAME_STUDENT_FUNC_ID))))
                .count();
        log.info("rename-sync exploration over {} run(s): {} showed an event-handler-induced "
                + "non-repeatable read, {} completed cleanly with a consistent summary",
                results.size(), staleSummaries, cleanlyConsistent);

        assertTrue(staleSummaries > 0,
                "driver should explore at least one interleaving where the name-sync handler's write lands "
                        + "between the summary's two tournament reads, a NON_REPEATABLE_READ whose interposing "
                        + "writer is an event handler");
        assertTrue(cleanlyConsistent > 0,
                "driver should also explore interleavings where both functionalities complete cleanly and "
                        + "the summary reads a single consistent version");
    }

    /**
     * A run in which the summary suffered a NON_REPEATABLE_READ on the tournament
     * whose interposing (second) writer is the name-sync event handler — i.e. an
     * anomaly genuinely induced by an event-handler write.
     */
    private static boolean isEventHandlerInducedStaleSummary(TestResult result) {
        List<StepId> handlerTournamentWrites = result.effectSequence().stream()
                .filter(effect -> effect.stepKind() == StepKind.EVENT_HANDLER && effect.isWrite())
                .filter(effect -> QuizzesTestFactory.TOURNAMENT_AGGREGATE_TYPE.equals(effect.aggregateType()))
                .map(StepEffect::stepId)
                .toList();

        return result.anomalies().stream()
                .anyMatch(anomaly -> anomaly instanceof Anomaly.NonRepeatableRead nonRepeatableRead
                        && nonRepeatableRead.functionality().equals(RENAME_SUMMARY_FUNC_ID)
                        && QuizzesTestFactory.TOURNAMENT_AGGREGATE_TYPE.equals(nonRepeatableRead.aggregateType())
                        && handlerTournamentWrites.contains(nonRepeatableRead.secondWriter()));
    }
}
