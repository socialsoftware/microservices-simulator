package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.WorkflowUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OracleQuizzesAppTest {
    private @Nullable Oracle oracle;
    private @Nullable QuizzesTestFactory factory;
    private @Nullable CommandGateway gateway;
    private @Nullable SagaUnitOfWorkService sagaUnitOfWorkService;


    // ======= Setup =======

    @BeforeAll
    void setupAll() {

        oracle = new Oracle(QuizzesSimulator.class, List.of());
        oracle.init();

        sagaUnitOfWorkService = oracle.getBean(SagaUnitOfWorkService.class);
        gateway = oracle.getBean(CommandGateway.class);

        factory = new QuizzesTestFactory(
                sagaUnitOfWorkService,
                oracle.getBean(ExecutionFunctionalities.class),
                oracle.getBean(UserFunctionalities.class),
                oracle.getBean(TopicFunctionalities.class),
                oracle.getBean(QuestionFunctionalities.class),
                oracle.getBean(TournamentFunctionalities.class));
    }

    @AfterAll
    void tearDownAll() {
        oracle.shutdown();
    }

    // ======= Tests =======

    @Test
    @DisplayName("Single run of one functionality should work")
    void singleRunOfFunctionalityShouldWork() {
        TestResult result = oracle.runTest(setupAddParticipantSagaInitialState);

        assertEquals(1, result.executedFunctionalities().size());
        WorkflowFunctionality executedSaga = result.executedFunctionalities().getFirst();
        var executedAddParticipantSaga = assertInstanceOf(
                AddParticipantFunctionalitySagas.class, executedSaga);

        UserDto readUserDto = executedAddParticipantSaga.getUserDto();
        assertEquals(QuizzesTestFactory.USER_NAME_1, readUserDto.getUsername());
        assertEquals(QuizzesTestFactory.USER_NAME_1, readUserDto.getName());
        // TODO fix this assert
        // assertEquals(QuizzesTestFactory.STUDENT_ROLE, readUserDto.getRole());

        List<FlowStep> expectedSchedule = WorkflowUtils.getWorkflowSteps(executedAddParticipantSaga.getWorkflow());
        assertTrue(result.exceptions().isEmpty());
        assertTrue(result.statuses().isEmpty());
        assertEquals(expectedSchedule, result.schedule());
    }

    @Test
    @DisplayName("Running the same test case consecutively returns the same test results because of inter-test isolation")
    void runningSameTestCaseConsecutivelyReturnsSameTestResults() {
        TestResult originalResult = oracle.runTest(setupAddParticipantSagaInitialState);
        assertEquals(0, originalResult.exceptions().size());
        List<String> originalStepNameSched = getStepNameSchedule(originalResult.schedule());

        final int consecutiveRuns = 50;
        for (int i = 0; i < consecutiveRuns; i++) {
            TestResult result = oracle.runTest(setupAddParticipantSagaInitialState);
            List<String> stepNameSched = getStepNameSchedule(result.schedule());

            assertEquals(originalResult.exceptions(), result.exceptions());
            assertEquals(originalResult.statuses(), result.statuses());
            assertEquals(originalStepNameSched, stepNameSched);
        }
    }
    @Test
    @DisplayName("When a step fails its consecutive steps (dependants) should not execute")
    void whenAStepFailsItsConsecutiveStepsShoulNotExecute() {
        Objects.requireNonNull(sagaUnitOfWorkService);
        SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork(
                AddParticipantFunctionalitySagas.class.getSimpleName());

        Supplier<TestCase> badAddParticipantSagaScenario = () -> {
            final int nonExistingAggregateId = 1;

            var addParticipantSaga = new AddParticipantFunctionalitySagas(
                    sagaUnitOfWorkService,
                    nonExistingAggregateId,
                    nonExistingAggregateId,
                    nonExistingAggregateId,
                    uow,
                    gateway);

            return new TestCase(List.of(addParticipantSaga), new StepDependencies());
        };

        TestResult result = oracle.runTest(badAddParticipantSagaScenario);

        assertEquals(1, result.executedFunctionalities().size());
        WorkflowFunctionality executedSaga = result.executedFunctionalities().getFirst();
        List<FlowStep> completeSchedule = WorkflowUtils.getWorkflowSteps(executedSaga.getWorkflow());
        List<FlowStep> brokenSchedule = completeSchedule.subList(0, 1);
        assertEquals(brokenSchedule, result.schedule());

        FlowStep brokenStep = brokenSchedule.getFirst();
        assertEquals(1, result.exceptions().size());
        assertInstanceOf(SimulatorException.class, result.exceptions().get(brokenStep));

        // TODO for now is wrong because its assuming schedule REJECTED
        // assertTrue(result.statuses().isEmpty());
    }

    // ======= Helper Functions =======

    private final Supplier<TestCase> setupAddParticipantSagaInitialState = () -> {
        var executionFunctionalities = oracle.getBean(ExecutionFunctionalities.class);
        AddParticipantFunctionalitySagas addParticipantSaga = factory
                .setupInitialStateAndCreateAddParticipantFunctionality(
                        sagaUnitOfWorkService,
                        gateway,
                        executionFunctionalities);

        return new TestCase(List.of(addParticipantSaga), new StepDependencies());
    };

    private List<String> getStepNameSchedule(List<FlowStep> schedule) {
        return schedule.stream().map(FlowStep::getName).toList();
    }
}
