package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Oracle;
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
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas;
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

    // TODO Should find a more relevant test case for the TestDriver to explore,
    // * where it's possible to assert "issues >= 1", to prove issues are found.

    // TODO When interInvariantsProvider for the example app is available, a test
    // * should be added to assert the tool finds inter-invariant violations
    // * autonomously.

    // TODO When a proper anomaly analyzer is implemented, tests that prove the
    // * tool finds anomalies autonomously should be added.

    @Test
    void driverExploresVariedInterleavings() {
        List<TestResult> results = driver.exploreTestCase(this::conflictingTournamentTestCase);

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

    @Test
    void driverDrivesFindingPipelineOverRacingSagas() {
        List<TestResult> results = driver.exploreTestCase(this::conflictingTournamentTestCase);

        assertEquals(ITERATIONS, results.size(),
                "finding pipeline should classify every run in the budget");

        List<TestResult> issues = results.stream().filter(TestDriverQuizzesAppTest::isIssue).toList();
        log.info("finding pipeline surfaced {} issue(s) over {} runs", issues.size(), results.size());
        issues.forEach(issue -> log.warn("  issue: statuses={}, exceptions={}",
                issue.statuses(), issue.exceptions().keySet()));
    }

    private static boolean isIssue(TestResult result) {
        return !result.exceptions().isEmpty()
                || result.statuses().contains(TestStatus.INTER_INVARIANT_VIOLATION)
                || result.statuses().contains(TestStatus.INTERNAL_SYSTEM_EXCEPTION)
                || result.statuses().contains(TestStatus.CRITICAL_STEP_FAILURE)
                || result.statuses().contains(TestStatus.EXECUTION_LIMIT_EXCEEDED);
    }

    private TestCase.Builder conflictingTournamentTestCase() {
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
}
