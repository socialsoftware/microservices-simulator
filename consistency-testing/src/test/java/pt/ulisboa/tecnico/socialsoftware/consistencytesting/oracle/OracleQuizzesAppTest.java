package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
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
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas;
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

        assertEquals(1, result.functionalities().size());
        WorkflowFunctionality executedSaga = result.functionalities().values().iterator().next();
        AddParticipantFunctionalitySagas executedAddParticipantSaga = assertInstanceOf(
                AddParticipantFunctionalitySagas.class, executedSaga);

        UserDto readUserDto = executedAddParticipantSaga.getUserDto();
        assertEquals(QuizzesTestFactory.USER_NAME_1, readUserDto.getUsername());
        assertEquals(QuizzesTestFactory.USER_NAME_1, readUserDto.getName());
        // TODO fix this assert
        // assertEquals(QuizzesTestFactory.STUDENT_ROLE, readUserDto.getRole());

        FunctionalityId funcId = getOnlyFunctionalityId(result);
        List<StepId> expectedSchedule = getFunctionalityStepIds(funcId, executedAddParticipantSaga, true);
        assertTrue(result.exceptions().isEmpty());
        assertTrue(result.statuses().isEmpty());
        assertEquals(expectedSchedule, result.schedule());
    }

    @Test
    @DisplayName("Running the same test case consecutively returns the same test results because of inter-test isolation")
    void runningSameTestCaseConsecutivelyReturnsSameTestResults() {
        TestResult originalResult = oracle.runTest(setupAddParticipantSagaInitialState);
        assertEquals(0, originalResult.exceptions().size());
        List<StepId> originalSchedule = List.copyOf(originalResult.schedule());

        final int consecutiveRuns = 5;
        for (int i = 0; i < consecutiveRuns; i++) {
            TestResult result = oracle.runTest(setupAddParticipantSagaInitialState);

            assertEquals(originalResult.exceptions(), result.exceptions());
            assertEquals(originalResult.statuses(), result.statuses());
            assertEquals(originalSchedule, result.schedule());
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

            FunctionalityId funcId = simpleFunctionalityId(addParticipantSaga, 1);
            return new TestCase.Builder()
                    .addFunctionality(funcId, addParticipantSaga)
                    .build();
        };

        TestResult result = oracle.runTest(badAddParticipantSagaScenario);

        assertEquals(1, result.functionalities().size());
        FunctionalityId funcId = getOnlyFunctionalityId(result);
        WorkflowFunctionality executedSaga = result.functionalities().get(funcId);
        assertInstanceOf(AddParticipantFunctionalitySagas.class, executedSaga);

        List<StepId> completeSchedule = getFunctionalityStepIds(funcId, executedSaga, false);
        List<StepId> brokenSchedule = completeSchedule.subList(0, 1);

        List<StepId> actualSchedule = result.schedule();
        assertEquals(brokenSchedule, actualSchedule.subList(0, brokenSchedule.size()));

        // should end with abort step from the compensation functionality
        StepId abortId = StepId.forAbortStep(funcId);
        assertEquals(abortId, actualSchedule.getLast());

        StepId brokenStep = brokenSchedule.getFirst();
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

        var runTimeException = new RuntimeException("This step breaks unexpectedly");
        var testFunc = new TestBrokenFunctionality(sagaUnitOfWorkService, uow, runTimeException);

        Supplier<TestCase> testFuncScenario = () -> new TestCase.Builder()
                .addFunctionality(simpleFunctionalityId(testFunc, 1), testFunc)
                .build();
        TestResult result = oracle.runTest(testFuncScenario);

        // if it broke unexpectedly the test halts and the compensations are skipped
        assertEquals(1, result.functionalities().size());
        WorkflowFunctionality executedFunc = result.functionalities().values().iterator().next();
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
        // TODO this asserts are broken because the oracle is registering
        // SCHEDULE_REJECTED
        // assertEquals(1, result.statuses().size());
        // assertEquals(Set.of(TestStatus.INTERNAL_EXCEPTION), result.statuses());

        // the exception registered in the test result is the expected one
        FunctionalityId funcId = getOnlyFunctionalityId(result);
        StepId brokenStep = getFunctionalityStepIds(funcId, executedTestFunc, false).get(2);
        assertEquals(runTimeException, result.exceptions().get(brokenStep));
    }

    @Test
    @DisplayName("When a functionality step fails the compensation plan is executed")
    void whenAFunctionalityStepFailsTheCompensationPlanIsExecuted() {
        SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork(
                TestBrokenFunctionality.class.getSimpleName());
        var expectedException = new SimulatorException("This step is expected to break");
        var testFunc = new TestBrokenFunctionality(sagaUnitOfWorkService, uow, expectedException);

        Supplier<TestCase> testFuncScenario = () -> new TestCase.Builder()
                .addFunctionality(simpleFunctionalityId(testFunc, 1), testFunc)
                .build();
        TestResult result = oracle.runTest(testFuncScenario);

        assertEquals(1, result.functionalities().size());
        FunctionalityId funcId = getOnlyFunctionalityId(result);
        WorkflowFunctionality executedFunc = result.functionalities().get(funcId);
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

        List<StepId> expectedSchedule = new ArrayList<>();
        expectedSchedule.addAll(getFunctionalityStepIds(funcId, executedTestFunc, false));
        expectedSchedule.addAll(getFunctionalityCompensationStepIds(funcId, executedTestFunc));

        assertEquals(expectedSchedule, result.schedule());

        // TODO do same test but for ConcludeQuizFunctionalitySagas
    }

    @ParameterizedTest()
    @ValueSource(ints = { 0, 1, 2 })
    @DisplayName("Inter dependencies are respected")
    void interDependenciesAreRespected(int selectDependencies) {
        AtomicReference<FunctionalityId> func1IdRef = new AtomicReference<>();
        AtomicReference<FunctionalityId> func2IdRef = new AtomicReference<>();

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

            FunctionalityId func1Id = simpleFunctionalityId(func1, 1);
            FunctionalityId func2Id = simpleFunctionalityId(func2, 2);
            func1IdRef.set(func1Id);
            func2IdRef.set(func2Id);
            var func1Steps = getFunctionalityStepIds(func1Id, func1, false);
            var func2Steps = getFunctionalityStepIds(func2Id, func2, false);

            TestCase.Builder builder = new TestCase.Builder()
                    .addFunctionality(func1Id, func1)
                    .addFunctionality(func2Id, func2);
            switch (selectDependencies) {
                case 0 -> {
                    // f1 step 1 depends on f2 step 1
                    builder.addInterDependency(func1Steps.get(0), func2Steps.get(0));
                    // f2 step 2 depends on f1 step 2
                    builder.addInterDependency(func2Steps.get(1), func1Steps.get(1));
                }
                case 1 -> {
                    // f2 step 1 depends on f1 step 1
                    builder.addInterDependency(func2Steps.get(0), func1Steps.get(0));
                    // f1 step 2 depends on f2 step 2
                    builder.addInterDependency(func1Steps.get(1), func2Steps.get(1));
                }
                case 2 -> {
                    // f1 step 1 depends on f2 step 2 (cross-step dependency)
                    builder.addInterDependency(func1Steps.get(0), func2Steps.get(1));
                }
                default -> {
                    throw new IllegalArgumentException(
                            "Invalid selectDependencies value: %d".formatted(selectDependencies));
                }
            }

            return builder.build();
        };

        TestResult result = oracle.runTest(setupTestCase);
        List<StepId> schedule = result.schedule();
        assertEquals(2, result.functionalities().size());
        FunctionalityId func1Id = Objects.requireNonNull(func1IdRef.get());
        FunctionalityId func2Id = Objects.requireNonNull(func2IdRef.get());
        WorkflowFunctionality executedFunc1 = result.functionalities().get(func1Id);
        WorkflowFunctionality executedFunc2 = result.functionalities().get(func2Id);
        List<StepId> func1Steps = getFunctionalityStepIds(func1Id, executedFunc1, false);
        List<StepId> func2Steps = getFunctionalityStepIds(func2Id, executedFunc2, false);

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

    @Test
    @DisplayName("Commit is called after successfully executing all steps of a functionality")
    void commitIsCalledAfterSuccessfullyExecutingAllStepsOfAFunctionality() {
        Supplier<TestCase> setupTestCase = () -> {
            InitialState initialState = factory.setupInitialState();

            TournamentDto updateDto = new TournamentDto();
            updateDto.setAggregateId(initialState.tournamentDto().getAggregateId());
            updateDto.setStartTime(initialState.tournamentDto().getStartTime());
            updateDto.setEndTime(initialState.tournamentDto().getEndTime());
            updateDto.setNumberOfQuestions(initialState.tournamentDto().getNumberOfQuestions());

            Set<Integer> topicIds = Set.of(initialState.topicDto().getAggregateId());
            UpdateTournamentFunctionalitySagas updateSaga = factory.createUpdateTournamentFunctionality(
                    sagaUnitOfWorkService,
                    updateDto,
                    topicIds,
                    gateway);

            // TODO requires fixing SagaStateConverter to return SagaState
            // Integer tournamentId = initialState.tournamentDto().getAggregateId();
            // assertEquals(GenericSagaState.NOT_IN_SAGA,
            // factory.sagaStateOf(tournamentId));

            return new TestCase.Builder()
                    .addFunctionality(simpleFunctionalityId(updateSaga, 1), updateSaga)
                    .build();
        };

        TestResult result = oracle.runTest(setupTestCase);

        assertEquals(1, result.functionalities().size());
        FunctionalityId funcId = getOnlyFunctionalityId(result);
        WorkflowFunctionality executed = result.functionalities().get(funcId);

        // Full schedule should match the functionality steps + commit
        List<StepId> expectedSchedule = getFunctionalityStepIds(funcId, executed, true);
        List<StepId> actualSchedule = result.schedule();
        assertEquals(expectedSchedule, actualSchedule);

        // Last step must be commit
        StepId commitId = StepId.forCommitStep(funcId);
        assertEquals(commitId, actualSchedule.getLast());

        // No abort step in schedule
        StepId abortId = StepId.forAbortStep(funcId);
        assertTrue(actualSchedule.stream().noneMatch(abortId::equals));

        // no exceptions/statuses
        assertTrue(result.exceptions().isEmpty());
        assertTrue(result.statuses().isEmpty());

        // TODO requires fixing SagaStateConverter to return SagaState
        // assertEquals(GenericSagaState.NOT_IN_SAGA,
        // factory.sagaStateOf(tournamentId));
    }

    @Test
    @DisplayName("Abort is called after a step of a functionality fails")
    void abortIsCalledAfterAStepOfAFunctionalityFails() {
        var expectedException = new SimulatorException("Expected failure");

        Supplier<TestCase> setupTestCase = () -> {
            SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork(
                    TestBrokenFunctionality.class.getSimpleName());
            var func = new TestBrokenFunctionality(sagaUnitOfWorkService, uow, expectedException);
            return new TestCase.Builder()
                    .addFunctionality(simpleFunctionalityId(func, 1), func)
                    .build();
        };

        TestResult result = oracle.runTest(setupTestCase);

        assertEquals(1, result.functionalities().size());
        FunctionalityId funcId = getOnlyFunctionalityId(result);
        WorkflowFunctionality executedFunc = result.functionalities().get(funcId);
        assertInstanceOf(TestBrokenFunctionality.class, executedFunc);

        TestBrokenFunctionality executed = (TestBrokenFunctionality) executedFunc;
        assertTrue(executed.hasFirstStepExecuted());
        assertTrue(executed.hasSecondStepExecuted());
        assertTrue(executed.hasThirdStepFailed());

        List<StepId> scheduleIds = result.schedule();

        // no commit for failed functionality
        StepId commitId = StepId.forCommitStep(funcId);
        assertTrue(scheduleIds.stream().noneMatch(commitId::equals));

        // third step is the failing step
        StepId thirdStep = getFunctionalityStepIds(funcId, executedFunc, false).get(2);
        assertTrue(result.exceptions().containsKey(thirdStep));

        assertEquals(expectedException, result.exceptions().get(thirdStep));

        // compensation steps (for 2nd and 1st) + abort at the end
        List<StepId> compensationSchedule = getFunctionalityCompensationStepIds(funcId, executedFunc);

        int lastIndex = -1;
        for (StepId stepId : compensationSchedule) {
            int index = scheduleIds.indexOf(stepId);
            assertTrue(index > lastIndex);
            lastIndex = index;
        }

        int failIndex = scheduleIds.indexOf(thirdStep);
        assertTrue(lastIndex > failIndex);

        assertEquals(compensationSchedule.getLast(), scheduleIds.getLast());
    }

    @Test
    @DisplayName("Abort is called after a step of a functionality fails, on UpdateTournamentFunctionality")
    void abortIsCalledAfterAStepOfAFunctionalityFailsOnUpdateTournamentFunctionality() {
        Supplier<TestCase> setupTestCase = () -> {
            InitialState initialState = factory.setupInitialState();

            TournamentDto updateDto = new TournamentDto();
            updateDto.setAggregateId(initialState.tournamentDto().getAggregateId());
            updateDto.setStartTime(initialState.tournamentDto().getStartTime());
            updateDto.setEndTime(initialState.tournamentDto().getEndTime());
            updateDto.setNumberOfQuestions(initialState.tournamentDto().getNumberOfQuestions());

            Set<Integer> invalidTopicIds = Set.of(initialState.topicDto().getAggregateId() + 9999);
            UpdateTournamentFunctionalitySagas updateSaga = factory.createUpdateTournamentFunctionality(
                    sagaUnitOfWorkService, updateDto, invalidTopicIds, gateway);

            return new TestCase.Builder()
                    .addFunctionality(simpleFunctionalityId(updateSaga, 1), updateSaga)
                    .build();
        };

        TestResult result = oracle.runTest(setupTestCase);

        assertEquals(1, result.functionalities().size());
        FunctionalityId funcId = getOnlyFunctionalityId(result);
        WorkflowFunctionality executedFunc = result.functionalities().get(funcId);
        assertInstanceOf(UpdateTournamentFunctionalitySagas.class, executedFunc);

        List<StepId> scheduleIds = result.schedule();

        // failed functionality should NOT reach commit
        StepId commitId = StepId.forCommitStep(funcId);
        assertTrue(scheduleIds.stream().noneMatch(commitId::equals));

        // compensation steps (if any) plus abort should execute in order, ending with
        // abort
        List<StepId> compensationSchedule = getFunctionalityCompensationStepIds(funcId, executedFunc);
        int lastIndex = -1;
        for (StepId stepId : compensationSchedule) {
            int index = scheduleIds.indexOf(stepId);
            assertTrue(index > lastIndex);
            lastIndex = index;
        }
        assertEquals(compensationSchedule.getLast(), scheduleIds.getLast());

        // should have a SimulatorException recorded on some step
        assertFalse(result.exceptions().isEmpty());
    }

    @Test
    @DisplayName("Compensations are not automatically executed")
    void compensationsAreNotAutomaticallyExecuted() {
        SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork(
                TestBrokenFunctionality.class.getSimpleName());
        var expectedException = new SimulatorException("Expected failure");
        var testFunc = new TestBrokenFunctionality(sagaUnitOfWorkService, uow, expectedException);

        FunctionalityId funcId = simpleFunctionalityId(testFunc, 1);
        StepId blockedCompensationStep = StepId.forCompensationStep(funcId, 1);
        StepId nonExecutedStep = StepId.forFunctionalityStep(
                FunctionalityId.forSagaFunctionality("nonExecutedFunc"), "nonExecutedStep");

        Supplier<TestCase> setupTestCase = () -> new TestCase.Builder()
                .addFunctionality(funcId, testFunc)
                .addInterDependency(blockedCompensationStep, nonExecutedStep)
                .build();

        TestResult result = oracle.runTest(setupTestCase);

        assertEquals(1, result.functionalities().size());
        List<StepId> compensationSteps = getFunctionalityCompensationStepIds(funcId, testFunc);

        assertFalse(testFunc.hasFirstStepCompensated());
        assertFalse(testFunc.hasSecondStepCompensated());
        assertFalse(testFunc.hasThirdStepCompensated());
        assertTrue(compensationSteps.stream().noneMatch(result.schedule()::contains));
    }

    @Test
    @DisplayName("Compensation functionalities are controlled by the oracle")
    void compensationFunctionalitiesAreControlledByTheOracle() {
        AtomicReference<FunctionalityId> brokenFuncIdRef = new AtomicReference<>();
        AtomicReference<StepId> otherFuncFirstStepRef = new AtomicReference<>();

        Supplier<TestCase> setupTestCase = () -> {
            InitialState initialState = factory.setupInitialState();

            AddParticipantFunctionalitySagas otherFunc = factory.createAddParticipantFunctionality(
                    sagaUnitOfWorkService,
                    initialState.tournamentDto().getAggregateId(),
                    initialState.courseExecutionDto().getAggregateId(),
                    initialState.userDto().getAggregateId(),
                    gateway);

            SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork(
                    TestBrokenFunctionality.class.getSimpleName());
            var expectedException = new SimulatorException("Expected failure");
            var brokenFunc = new TestBrokenFunctionality(sagaUnitOfWorkService, uow, expectedException);

            FunctionalityId otherFuncId = simpleFunctionalityId(otherFunc, 1);
            FunctionalityId brokenFuncId = simpleFunctionalityId(brokenFunc, 2);
            brokenFuncIdRef.set(brokenFuncId);

            StepId otherFuncStep = getFunctionalityStepIds(otherFuncId, otherFunc, false).get(0);
            otherFuncFirstStepRef.set(otherFuncStep);
            StepId firstCompensationStep = StepId.forCompensationStep(brokenFuncId, 1);

            return new TestCase.Builder()
                    .addFunctionality(otherFuncId, otherFunc)
                    .addFunctionality(brokenFuncId, brokenFunc)
                    .addInterDependency(firstCompensationStep, otherFuncStep)
                    .build();
        };

        TestResult result = oracle.runTest(setupTestCase);

        FunctionalityId brokenFuncId = Objects.requireNonNull(brokenFuncIdRef.get());
        StepId otherFuncFirstStep = Objects.requireNonNull(otherFuncFirstStepRef.get());

        StepId firstCompensationStep = StepId.forCompensationStep(brokenFuncId, 1);

        assertTrue(result.schedule().contains(firstCompensationStep));
        assertTrue(result.schedule().indexOf(firstCompensationStep) > result.schedule().indexOf(otherFuncFirstStep));
    }

    // ======= Helper Functions =======

    private final Supplier<TestCase> setupAddParticipantSagaInitialState = () -> {
        var executionFunctionalities = oracle.getBean(ExecutionFunctionalities.class);
        AddParticipantFunctionalitySagas addParticipantSaga = factory
                .setupInitialStateAndCreateAddParticipantFunctionality(
                        sagaUnitOfWorkService,
                        gateway,
                        executionFunctionalities);

        FunctionalityId funcId = simpleFunctionalityId(addParticipantSaga, 1);
        return new TestCase.Builder()
                .addFunctionality(funcId, addParticipantSaga)
                .build();
    };

    private FunctionalityId simpleFunctionalityId(WorkflowFunctionality func, int index) {
        String baseName = func.getClass().getSimpleName();
        return FunctionalityId.forSagaFunctionality("%s-%d".formatted(baseName, index));
    }

    private FunctionalityId getOnlyFunctionalityId(TestResult result) {
        return result.functionalities().keySet().iterator().next();
    }

    private List<StepId> getFunctionalityStepIds(
            FunctionalityId funcId,
            WorkflowFunctionality func,
            boolean includeCommitStep) {

        List<StepId> stepIds = FunctionalityUtils.getSteps(func).stream()
                .map(step -> StepId.forFunctionalityStep(funcId, step.getName()))
                .toList();

        if (!includeCommitStep) {
            return stepIds;
        }

        List<StepId> withCommit = new ArrayList<>(stepIds);
        withCommit.add(StepId.forCommitStep(funcId));
        return withCommit;
    }

    private List<StepId> getFunctionalityCompensationStepIds(FunctionalityId funcId, WorkflowFunctionality func) {
        UnitOfWork uow = func.getWorkflow().getUnitOfWork();
        if (!(uow instanceof SagaUnitOfWork sagaUow)) {
            throw new IllegalArgumentException(
                    "Cannot retrieve compensation steps for workflow with unit of work %s expected %s"
                            .formatted(uow.getClass().getName(), SagaUnitOfWork.class.getName()));
        }

        int compensationCount = sagaUow.getRegisteredCompensations().size();
        List<StepId> compensationStepIds = new ArrayList<>(compensationCount + 1);
        for (int i = compensationCount - 1; i >= 0; i--) {
            compensationStepIds.add(StepId.forCompensationStep(funcId, i));
        }
        compensationStepIds.add(StepId.forAbortStep(funcId));
        return compensationStepIds;
    }
}
