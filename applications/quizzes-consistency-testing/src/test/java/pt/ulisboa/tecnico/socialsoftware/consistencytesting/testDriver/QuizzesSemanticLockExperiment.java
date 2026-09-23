package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantWithinMaxTournamentsFunctionalitySagas.MAX_TOURNAMENTS_PER_USER;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Oracle;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.SemanticLockActivity;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.SemanticLockId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestCase;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;
import pt.ulisboa.tecnico.socialsoftware.quizzes.oracle.InitialState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.oracle.QuizzesTestFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.states.CourseExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.states.TournamentSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantWithinMaxTournamentsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.MoveParticipantBetweenTournamentsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;

/**
 * Paired positive-control experiments for two application-level semantic locks.
 * Each pair uses the same initial state and forced ordering; only enforcement
 * of the selected lock changes.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuizzesSemanticLockExperiment {

    private static final FunctionalityId QUOTA_JOIN_A =
            FunctionalityId.forSagaFunctionality("quotaJoinTournamentA");
    private static final FunctionalityId QUOTA_JOIN_B =
            FunctionalityId.forSagaFunctionality("quotaJoinTournamentB");
    private static final FunctionalityId MOVE =
            FunctionalityId.forSagaFunctionality("moveParticipant");
    private static final FunctionalityId REMOVE_SOURCE =
            FunctionalityId.forSagaFunctionality("removeSourceTournament");

    private static final SemanticLockId QUOTA_LOCK =
            SemanticLockId.from(CourseExecutionSagaState.IN_TOURNAMENT_QUOTA_UPDATE);
    private static final SemanticLockId MOVE_LOCK =
            SemanticLockId.from(TournamentSagaState.IN_MOVE_PARTICIPANT);

    private TestDriver driver;
    private Oracle oracle;
    private QuizzesTestFactory factory;

    private Integer quotaTournamentA;
    private Integer quotaTournamentB;
    private Integer quotaJoiner;
    private Integer sourceTournament;
    private Integer targetTournament;
    private Integer movedUser;

    @BeforeAll
    void startOracle() {
        driver = new TestDriver(
                QuizzesSimulator.class,
                List.of(),
                Path.of("target", "semantic-lock-experiment-reports"));
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

    @AfterEach
    void restoreLocks() {
        driver.setIgnoredSemanticLocks(Set.of());
    }

    @AfterAll
    void stopOracle() {
        driver.shutdown();
    }

    @Test
    void quotaLockPreventsViolationUnderForcedWriteSkewSchedule() {
        driver.setIgnoredSemanticLocks(Set.of());
        AtomicReference<Integer> finalCount = new AtomicReference<>();

        TestResult result = oracle.runTest(forcedQuotaWriteSkew(),
                ignored -> finalCount.set(quotaParticipationCount()));

        assertFalse(result.statuses().contains(TestStatus.INTER_INVARIANT_VIOLATION));
        assertTrue(finalCount.get() <= MAX_TOURNAMENTS_PER_USER);
        assertTrue(hasLockOutcome(result, QUOTA_LOCK, SemanticLockActivity.Outcome.ACQUIRED));
        assertTrue(hasLockOutcome(result, QUOTA_LOCK, SemanticLockActivity.Outcome.REJECTED));
    }

    @Test
    void ignoringQuotaLockCausesInterInvariantViolationUnderSameSchedule() {
        driver.setIgnoredSemanticLocks(Set.of(QUOTA_LOCK));
        AtomicReference<Integer> finalCount = new AtomicReference<>();

        TestResult result = oracle.runTest(forcedQuotaWriteSkew(),
                ignored -> finalCount.set(quotaParticipationCount()));

        assertTrue(result.statuses().contains(TestStatus.INTER_INVARIANT_VIOLATION));
        assertTrue(result.interInvariantViolations().containsKey("MAX_TOURNAMENTS_PER_USER"));
        assertTrue(finalCount.get() > MAX_TOURNAMENTS_PER_USER);
        assertTrue(hasLockOutcome(result, QUOTA_LOCK, SemanticLockActivity.Outcome.SKIPPED));
    }

    @Test
    void moveLockKeepsSourceAvailableForCompensation() {
        driver.setIgnoredSemanticLocks(Set.of());
        AtomicReference<MoveOutcome> outcome = new AtomicReference<>();

        TestResult result = oracle.runTest(protectedImpossibleCompensation(),
                ignored -> outcome.set(readMoveOutcome()));

        assertFalse(result.statuses().contains(TestStatus.CRITICAL_STEP_FAILURE));
        assertTrue(outcome.get().sourceExists());
        assertTrue(outcome.get().userInSource());
        assertFalse(outcome.get().userInTarget());
        assertTrue(hasLockOutcome(result, MOVE_LOCK, SemanticLockActivity.Outcome.ACQUIRED));
        assertTrue(hasLockOutcome(result, MOVE_LOCK, SemanticLockActivity.Outcome.REJECTED));
    }

    @Test
    void ignoringMoveLockMakesCompensationImpossible() {
        driver.setIgnoredSemanticLocks(Set.of(MOVE_LOCK));
        AtomicReference<MoveOutcome> outcome = new AtomicReference<>();

        TestResult result = oracle.runTest(forcedImpossibleCompensation(),
                ignored -> outcome.set(readMoveOutcome()));

        assertTrue(result.statuses().contains(TestStatus.CRITICAL_STEP_FAILURE));
        assertFalse(outcome.get().sourceExists());
        assertFalse(outcome.get().userInSource());
        assertFalse(outcome.get().userInTarget());
        assertTrue(hasLockOutcome(result, MOVE_LOCK, SemanticLockActivity.Outcome.SKIPPED));
    }

    private Supplier<TestCase> forcedQuotaWriteSkew() {
        StepId joinACount = StepId.forFunctionalityStep(QUOTA_JOIN_A, "countUserTournamentsStep");
        StepId joinAAdd = StepId.forFunctionalityStep(QUOTA_JOIN_A, "addParticipantStep");
        StepId joinBCount = StepId.forFunctionalityStep(QUOTA_JOIN_B, "countUserTournamentsStep");
        StepId joinBAdd = StepId.forFunctionalityStep(QUOTA_JOIN_B, "addParticipantStep");
        return () -> quotaTestCase()
                .addInterDependency(joinAAdd, joinBCount)
                .addInterDependency(joinBAdd, joinACount)
                .build();
    }

    private TestCase.Builder quotaTestCase() {
        InitialState state = factory.setupInitialState();
        Integer executionId = state.courseExecutionDto().getAggregateId();
        Integer topicId = state.topicDto().getAggregateId();
        Integer creatorId = state.userDto().getAggregateId();

        quotaTournamentA = state.tournamentDto().getAggregateId();
        quotaTournamentB = factory.createTournament(
                QuizzesTestFactory.time1(), QuizzesTestFactory.time3(), 1,
                creatorId, executionId, List.of(topicId)).getAggregateId();
        quotaJoiner = factory.createStudentInExecution(
                executionId, QuizzesTestFactory.USER_NAME_2, QuizzesTestFactory.USER_USERNAME_2)
                .getAggregateId();

        SagaUnitOfWorkService unitOfWorkService = oracle.getBean(SagaUnitOfWorkService.class);
        CommandGateway gateway = oracle.getBean(CommandGateway.class);
        AddParticipantWithinMaxTournamentsFunctionalitySagas joinA =
                factory.createAddParticipantWithinMaxTournamentsFunctionality(
                        unitOfWorkService, quotaTournamentA, executionId, quotaJoiner, gateway);
        AddParticipantWithinMaxTournamentsFunctionalitySagas joinB =
                factory.createAddParticipantWithinMaxTournamentsFunctionality(
                        unitOfWorkService, quotaTournamentB, executionId, quotaJoiner, gateway);

        return new TestCase.Builder()
                .addFunctionality(QUOTA_JOIN_A, joinA)
                .addFunctionality(QUOTA_JOIN_B, joinB);
    }

    private Supplier<TestCase> protectedImpossibleCompensation() {
        StepId leaveSource = StepId.forFunctionalityStep(MOVE, "leaveSourceTournamentStep");
        StepId removeRead = StepId.forFunctionalityStep(REMOVE_SOURCE, "getTournamentStep");
        return () -> impossibleCompensationTestCase()
                .addInterDependency(removeRead, leaveSource)
                .build();
    }

    private Supplier<TestCase> forcedImpossibleCompensation() {
        StepId leaveSource = StepId.forFunctionalityStep(MOVE, "leaveSourceTournamentStep");
        StepId addToTarget = StepId.forFunctionalityStep(MOVE, "addToTargetTournamentStep");
        StepId removeRead = StepId.forFunctionalityStep(REMOVE_SOURCE, "getTournamentStep");
        StepId remove = StepId.forFunctionalityStep(REMOVE_SOURCE, "removeTournamentStep");
        return () -> impossibleCompensationTestCase()
                .addInterDependency(removeRead, leaveSource)
                .addInterDependency(addToTarget, remove)
                .build();
    }

    private TestCase.Builder impossibleCompensationTestCase() {
        InitialState state = factory.setupInitialState();
        Integer executionId = state.courseExecutionDto().getAggregateId();
        Integer topicId = state.topicDto().getAggregateId();
        Integer creatorId = state.userDto().getAggregateId();

        sourceTournament = state.tournamentDto().getAggregateId();
        targetTournament = factory.createStartedTournament(
                creatorId, executionId, List.of(topicId)).getAggregateId();
        movedUser = factory.createStudentInExecution(
                executionId, QuizzesTestFactory.USER_NAME_3, QuizzesTestFactory.USER_USERNAME_3)
                .getAggregateId();
        factory.addParticipant(sourceTournament, executionId, movedUser);

        SagaUnitOfWorkService unitOfWorkService = oracle.getBean(SagaUnitOfWorkService.class);
        CommandGateway gateway = oracle.getBean(CommandGateway.class);
        MoveParticipantBetweenTournamentsFunctionalitySagas move =
                factory.createMoveParticipantBetweenTournamentsFunctionality(
                        unitOfWorkService, sourceTournament, targetTournament,
                        executionId, movedUser, gateway);
        RemoveTournamentFunctionalitySagas removeSource =
                factory.createRemoveTournamentFunctionality(
                        unitOfWorkService, sourceTournament, gateway);

        return new TestCase.Builder()
                .addFunctionality(MOVE, move)
                .addFunctionality(REMOVE_SOURCE, removeSource);
    }

    private int quotaParticipationCount() {
        return (participantIsPresent(quotaTournamentA, quotaJoiner) ? 1 : 0)
                + (participantIsPresent(quotaTournamentB, quotaJoiner) ? 1 : 0);
    }

    private MoveOutcome readMoveOutcome() {
        return new MoveOutcome(
                participantIsPresent(sourceTournament, movedUser),
                participantIsPresent(targetTournament, movedUser),
                tournamentExists(sourceTournament));
    }

    private boolean participantIsPresent(Integer tournamentId, Integer userId) {
        try {
            TournamentDto tournament = oracle.getBean(TournamentFunctionalities.class)
                    .findTournament(tournamentId);
            return tournament != null && tournament.findParticipant(userId) != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean tournamentExists(Integer tournamentId) {
        try {
            return oracle.getBean(TournamentFunctionalities.class).findTournament(tournamentId) != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean hasLockOutcome(
            TestResult result,
            SemanticLockId lock,
            SemanticLockActivity.Outcome outcome) {
        return result.semanticLockTrace().stream()
                .anyMatch(activity -> activity.semanticLock().equals(lock) && activity.outcome() == outcome);
    }

    private record MoveOutcome(boolean userInSource, boolean userInTarget, boolean sourceExists) {
    }
}
