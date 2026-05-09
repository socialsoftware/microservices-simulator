package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.FunctionalityUtils;
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

        // TODO should test with the real app args?
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

        List<FlowStep> expectedSchedule = FunctionalityUtils.getSteps(executedAddParticipantSaga);
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
    @DisplayName("When a step fails its consecutive functionality steps (intra-dependants) should not execute")
    void whenAStepFailsItsConsecutiveFunctionalityStepsShoulNotExecute() {
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

        assertEquals(2, result.executedFunctionalities().size());
        WorkflowFunctionality executedSaga = result.executedFunctionalities().get(0);
        WorkflowFunctionality compensationSaga = result.executedFunctionalities().get(1);

        assertInstanceOf(AddParticipantFunctionalitySagas.class, executedSaga);
        assertInstanceOf(CompensationFunctionality.class, compensationSaga);

        List<FlowStep> completeSchedule = FunctionalityUtils.getSteps(executedSaga);
        List<FlowStep> brokenSchedule = completeSchedule.subList(0, 1);
        assertEquals(brokenSchedule, result.schedule());

        FlowStep brokenStep = brokenSchedule.getFirst();
        assertEquals(1, result.exceptions().size());
        assertInstanceOf(SimulatorException.class, result.exceptions().get(brokenStep));

        // TODO for now is wrong because its assuming schedule REJECTED
        // assertTrue(result.statuses().isEmpty());
    }

    @Test
    @DisplayName("When a functionality step fails with a unexpected exception the test result registers INTERNAL_ERROR status")
    void whenAFunctionalityStepFailsWithAUnexpectedExceptionTheTestResultRegistersInternalErrorStatus() {
        SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork(
                TestBrokenFunctionality.class.getSimpleName());

        var runTimeException = new RuntimeException("This step is breaks unexpectedly");
        var testFunc = new TestBrokenFunctionality(sagaUnitOfWorkService, uow, runTimeException);

        Supplier<TestCase> testFuncScenario = () -> new TestCase(List.of(testFunc), new StepDependencies());
        TestResult result = oracle.runTest(testFuncScenario);

        // if it broke unexpectedly the test halts and the compensations are skipped
        assertEquals(1, result.executedFunctionalities().size());
        WorkflowFunctionality executedFunc = result.executedFunctionalities().getFirst();
        assertFalse(executedFunc instanceof CompensationFunctionality);
        assertInstanceOf(TestBrokenFunctionality.class, executedFunc);
        TestBrokenFunctionality executedTestFunc = (TestBrokenFunctionality) executedFunc;

        assertTrue(executedTestFunc.hasFirstStepExecuted());
        assertTrue(executedTestFunc.hasSecondStepExecuted());
        assertTrue(executedTestFunc.hasThirdStepFailed());

        // third step does not compensate because it failed (nothing to compensate)
        assertFalse(executedTestFunc.hasThirdStepCompensated());

        // second and first step don't compensate because test halted
        assertFalse(executedTestFunc.hasSecondStepCompensated());
        assertFalse(executedTestFunc.hasFirstStepCompensated());

        // INTERNAL_ERROR status is registered
        assertEquals(1, result.statuses().size());
        assertEquals(Set.of(TestStatus.INTERNAL_EXCEPTION), result.statuses());

        // the exception registered in the test result is the expected one
        FlowStep brokenStep = FunctionalityUtils.getSteps(executedTestFunc).get(2);
        assertEquals(runTimeException, result.exceptions().get(brokenStep));
    }

    @Test
    @DisplayName("When a functionality step fails the compensation plan is executed")
    void whenAFunctionalityStepFailsTheCompensationPlanIsExecuted() {
        SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork(
                TestBrokenFunctionality.class.getSimpleName());
        var expectedException = new SimulatorException("This step is expected to break");
        var testFunc = new TestBrokenFunctionality(sagaUnitOfWorkService, uow, expectedException);

        Supplier<TestCase> testFuncScenario = () -> new TestCase(List.of(testFunc), new StepDependencies());
        TestResult result = oracle.runTest(testFuncScenario);

        assertEquals(2, result.executedFunctionalities().size());
        WorkflowFunctionality executedFunc = result.executedFunctionalities().get(0);
        WorkflowFunctionality compensationFunc = result.executedFunctionalities().get(1);

        assertInstanceOf(CompensationFunctionality.class, compensationFunc);
        assertInstanceOf(TestBrokenFunctionality.class, executedFunc);
        TestBrokenFunctionality executedTestFunc = (TestBrokenFunctionality) executedFunc;

        assertTrue(executedTestFunc.hasFirstStepExecuted());
        assertTrue(executedTestFunc.hasSecondStepExecuted());
        assertTrue(executedTestFunc.hasThirdStepFailed());

        // third step does not compensate because it failed (nothing to compensate)
        assertFalse(executedTestFunc.hasThirdStepCompensated());

        // second and first step compensate because they had been succesfully executed
        assertTrue(executedTestFunc.hasSecondStepCompensated());
        assertTrue(executedTestFunc.hasFirstStepCompensated());

        List<String> scheduleStepNames = result.schedule().stream().map(FlowStep::getName).toList();
        List<String> expectedSchedule = List.of(
                TestBrokenFunctionality.FIRST_STEP_NAME,
                TestBrokenFunctionality.SECOND_STEP_NAME,
                TestBrokenFunctionality.THIRD_STEP_NAME,
                FunctionalityUtils.getCompensationStepName(1), // compensation of second step
                FunctionalityUtils.getCompensationStepName(0) // compensation of first step
        );

        assertEquals(expectedSchedule, scheduleStepNames);

        // TODO do same test but for ConcludeQuizFunctionalitySagas
    }

    @ParameterizedTest()
    @ValueSource(ints = { 0, 1, 2 })
    @DisplayName("Inter dependencies are respected")
    void interDependenciesAreRespected(int selectDependencies) {
        Supplier<TestCase> setupTestCase = () -> {
            InitialState initialState = factory.setupInitialState();

            AddParticipantFunctionalitySagas func1 = factory.createAddParticipantFunctionality(
                    sagaUnitOfWorkService,
                    initialState.tournamentDto().getAggregateId(),
                    initialState.courseExecutionDto().getAggregateId(),
                    initialState.userDto().getAggregateId(),
                    gateway);

            AddParticipantFunctionalitySagas func2 = factory.createAddParticipantFunctionality(
                    sagaUnitOfWorkService,
                    initialState.tournamentDto().getAggregateId(),
                    initialState.courseExecutionDto().getAggregateId(),
                    initialState.userDto().getAggregateId(),
                    gateway);

            var func1Steps = FunctionalityUtils.getSteps(func1);
            var func2Steps = FunctionalityUtils.getSteps(func2);

            StepDependencies dependencies = new StepDependencies();
            switch (selectDependencies) {
                case 0 -> {
                    // f1 step 1 depends on f2 step 1
                    dependencies.setStepDependencies(func1Steps.get(0), Set.of(func2Steps.get(0)));
                    // f2 step 2 depends on f1 step 2
                    dependencies.setStepDependencies(func2Steps.get(1), Set.of(func1Steps.get(1)));
                }
                case 1 -> {
                    // f2 step 1 depends on f1 step 1
                    dependencies.setStepDependencies(func2Steps.get(0), Set.of(func1Steps.get(0)));
                    // f1 step 2 depends on f2 step 2
                    dependencies.setStepDependencies(func1Steps.get(1), Set.of(func2Steps.get(1)));
                }
                case 2 -> {
                    // f1 step 1 depends on f2 step 2 (cross-step dependency)
                    dependencies.setStepDependencies(func1Steps.get(0), Set.of(func2Steps.get(1)));
                }
                default -> {
                    throw new IllegalArgumentException(
                            "Invalid selectDependencies value: %d".formatted(selectDependencies));
                }
            }

            return new TestCase(List.of(func1, func2), dependencies);
        };

        TestResult result = oracle.runTest(setupTestCase);
        List<FlowStep> schedule = result.schedule();
        assertEquals(2, result.executedFunctionalities().size());
        WorkflowFunctionality executedFunc1 = result.executedFunctionalities().get(0);
        WorkflowFunctionality executedFunc2 = result.executedFunctionalities().get(1);
        List<FlowStep> func1Steps = FunctionalityUtils.getSteps(executedFunc1);
        List<FlowStep> func2Steps = FunctionalityUtils.getSteps(executedFunc2);

        switch (selectDependencies) {
            case 0 -> {
                // f1 step 1 depends on f2 step 1
                assertTrue(schedule.indexOf(func1Steps.get(0)) > schedule.indexOf(func2Steps.get(0)));
                // f2 step 2 depends on f1 step 2
                assertTrue(schedule.indexOf(func2Steps.get(1)) > schedule.indexOf(func1Steps.get(1)));
            }
            case 1 -> {
                // f2 step 1 depends on f1 step 1
                assertTrue(schedule.indexOf(func2Steps.get(0)) > schedule.indexOf(func1Steps.get(0)));
                // f1 step 2 depends on f2 step 2
                assertTrue(schedule.indexOf(func1Steps.get(1)) > schedule.indexOf(func2Steps.get(1)));
            }
            case 2 -> {
                // f1 step 1 depends on f2 step 2 (cross-step dependency)
                assertTrue(schedule.indexOf(func1Steps.get(0)) > schedule.indexOf(func2Steps.get(1)));
            }
            default -> {
                throw new IllegalArgumentException(
                        "Invalid selectDependencies value: %d".formatted(selectDependencies));
            }
        }
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
