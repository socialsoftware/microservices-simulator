package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.behaviour

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorBoundaryContext
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorFault
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorInjectedFaultException
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorProviderHolder
import pt.ulisboa.tecnico.socialsoftware.ms.faults.InMemoryFaultVectorProvider
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceEvent
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorder
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorderHolder
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.StartQuizFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.sagas.states.QuizSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.CreateTournamentFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

import java.util.concurrent.CompletionException

@DataJpaTest
class CreateTournamentStartQuizRecoveryWindowExploratoryTest extends QuizzesSpockTest {
    private static final List<String> CREATE_PREFIX = [
            'getCourseExecutionStep',
            'getCreatorStep',
            'getTopicsStep',
            'findQuestionsByTopicIdsStep',
            'getCourseExecutionById',
            'generateQuizStep'
    ]

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService
    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private QuizFunctionalities quizFunctionalities
    @Autowired
    private QuizAnswerRepository quizAnswerRepository
    @Autowired
    private LocalCommandGateway commandGateway

    private CourseExecutionDto courseExecutionDto
    private UserDto creatorDto
    private UserDto studentDto
    private TopicDto topicDto1
    private TopicDto topicDto2
    private QuestionDto questionDto1
    private QuestionDto questionDto2
    private TournamentDto tournamentInput
    private RecordingInvariantRecorder invariantRecorder
    private DynamicEvidenceRecorderHolder.Scope invariantRecorderScope

    def setup() {
        FaultVectorProviderHolder.clear()
        invariantRecorder = new RecordingInvariantRecorder(DynamicEvidenceRecorderHolder.recorder)
        invariantRecorderScope = DynamicEvidenceRecorderHolder.install(invariantRecorder)

        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE,
                COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)
        creatorDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        studentDto = createUser(USER_NAME_2, USER_USERNAME_2, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, creatorDto.aggregateId)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, studentDto.aggregateId)

        topicDto1 = createTopic(courseExecutionDto, TOPIC_NAME_1)
        topicDto2 = createTopic(courseExecutionDto, TOPIC_NAME_2)
        questionDto1 = createQuestion(courseExecutionDto, [topicDto1] as Set, TITLE_1, CONTENT_1, OPTION_1, OPTION_2)
        questionDto2 = createQuestion(courseExecutionDto, [topicDto2] as Set, TITLE_2, CONTENT_2, OPTION_3, OPTION_4)

        tournamentInput = new TournamentDto(
                startTime: DateHandler.toISOString(TIME_1),
                endTime: DateHandler.toISOString(TIME_3),
                numberOfQuestions: 2)
    }

    def cleanup() {
        FaultVectorProviderHolder.clear()
        invariantRecorderScope?.close()
    }

    def 'control: successful CreateTournament followed by StartQuiz is safe'() {
        given:
        def create = newCreateTournament()

        when:
        executeCreatePrefix(create.functionality, create.unitOfWork)
        create.functionality.executeStepForExecutor('createTournamentStep', create.unitOfWork)
        def createFinalization = create.functionality.finalizeForExecutor(create.unitOfWork)
        def quizId = create.functionality.quizDto.aggregateId
        def start = newStartQuiz(quizId)
        executeStartQuiz(start.functionality, start.unitOfWork)
        def answerId = start.functionality.quizAnswerDto.aggregateId

        then:
        createFinalization.success()
        create.unitOfWork.executedSteps == CREATE_PREFIX + ['createTournamentStep']
        quizFunctionalities.findQuiz(quizId).aggregateId == quizId
        def answer = quizAnswerRepository.findLastAggregateVersion(answerId).orElseThrow()
        answer.quiz.quizAggregateId == quizId
        answer.state == Aggregate.AggregateState.ACTIVE
        sagaStateOf(quizId) == GenericSagaState.NOT_IN_SAGA
        sagaStateOf(answerId) == GenericSagaState.NOT_IN_SAGA
        invariantRecorder.invariantViolations.empty
    }

    def 'early compensation: createTournamentStep fault removes Quiz before StartQuiz can read it'() {
        given:
        def create = newCreateTournament()
        executeCreatePrefix(create.functionality, create.unitOfWork)
        def quizId = create.functionality.quizDto.aggregateId

        when:
        def fault = injectCreateTournamentFault(create.functionality, create.unitOfWork)

        then:
        fault instanceof FaultVectorInjectedFaultException
        fault.runtimeStepName == 'createTournamentStep'
        create.unitOfWork.executedSteps == CREATE_PREFIX
        quizFunctionalities.findQuiz(quizId).aggregateId == quizId

        when:
        create.functionality.resumeCompensation(create.unitOfWork)

        then:
        create.unitOfWork.isCompensationExecuted('generateQuizStep')
        unitOfWorkService.aggregateDeletedLoad(quizId).state == Aggregate.AggregateState.DELETED
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
        sagaStateOf(topicDto1.aggregateId) == GenericSagaState.NOT_IN_SAGA
        sagaStateOf(topicDto2.aggregateId) == GenericSagaState.NOT_IN_SAGA

        when:
        def start = newStartQuiz(quizId)
        start.functionality.executeStepForExecutor('getQuizStep', start.unitOfWork)

        then:
        def failure = thrown(CompletionException)
        failure.cause.message.contains(quizId.toString())
        quizAnswerRepository.findAllAggregateIds().empty
        invariantRecorder.invariantViolations.empty
    }

    def 'late compensation: completed StartQuiz leaves an active QuizAnswer referring to deleted Quiz'() {
        given:
        def create = newCreateTournament()
        executeCreatePrefix(create.functionality, create.unitOfWork)
        def quizId = create.functionality.quizDto.aggregateId
        def fault = injectCreateTournamentFault(create.functionality, create.unitOfWork)
        assert fault instanceof FaultVectorInjectedFaultException

        when:
        def start = newStartQuiz(quizId)
        executeStartQuiz(start.functionality, start.unitOfWork)
        def answerId = start.functionality.quizAnswerDto.aggregateId
        create.functionality.resumeCompensation(create.unitOfWork)

        then:
        create.unitOfWork.isCompensationExecuted('generateQuizStep')
        unitOfWorkService.aggregateDeletedLoad(quizId).state == Aggregate.AggregateState.DELETED
        def answer = quizAnswerRepository.findLastAggregateVersion(answerId).orElseThrow()
        answer.state == Aggregate.AggregateState.ACTIVE
        answer.quiz.quizAggregateId == quizId
        sagaStateOf(answerId) == GenericSagaState.NOT_IN_SAGA
        invariantRecorder.invariantViolations.empty
    }

    def 'split StartQuiz: cached Quiz survives deletion and StartQuiz resumes to create a dangling answer'() {
        given:
        def create = newCreateTournament()
        executeCreatePrefix(create.functionality, create.unitOfWork)
        def quizId = create.functionality.quizDto.aggregateId
        def fault = injectCreateTournamentFault(create.functionality, create.unitOfWork)
        assert fault instanceof FaultVectorInjectedFaultException
        def start = newStartQuiz(quizId)

        when:
        start.functionality.executeStepForExecutor('getQuizStep', start.unitOfWork)

        then:
        start.unitOfWork.executedSteps == ['getQuizStep']
        sagaStateOf(quizId) == QuizSagaState.READ_QUIZ

        when:
        create.functionality.resumeCompensation(create.unitOfWork)

        then:
        create.unitOfWork.isCompensationExecuted('generateQuizStep')
        unitOfWorkService.aggregateDeletedLoad(quizId).state == Aggregate.AggregateState.DELETED

        when:
        start.functionality.executeStepForExecutor('getUserStep', start.unitOfWork)
        start.functionality.executeStepForExecutor('startQuizStep', start.unitOfWork)
        def startFinalization = start.functionality.finalizeForExecutor(start.unitOfWork)
        def answerId = start.functionality.quizAnswerDto.aggregateId

        then:
        startFinalization.success()
        start.unitOfWork.executedSteps == ['getQuizStep', 'getUserStep', 'startQuizStep']
        def deletedQuiz = unitOfWorkService.aggregateDeletedLoad(quizId)
        deletedQuiz.state == Aggregate.AggregateState.DELETED
        deletedQuiz.sagaState == GenericSagaState.NOT_IN_SAGA
        def answer = quizAnswerRepository.findLastAggregateVersion(answerId).orElseThrow()
        answer.state == Aggregate.AggregateState.ACTIVE
        answer.quiz.quizAggregateId == quizId
        sagaStateOf(answerId) == GenericSagaState.NOT_IN_SAGA
        invariantRecorder.invariantViolations.empty
    }

    private Map newCreateTournament() {
        def unitOfWork = unitOfWorkService.createUnitOfWork(CreateTournamentFunctionalitySagas.simpleName)
        def functionality = new CreateTournamentFunctionalitySagas(
                unitOfWorkService,
                creatorDto.aggregateId,
                courseExecutionDto.aggregateId,
                [topicDto1.aggregateId, topicDto2.aggregateId],
                tournamentInput,
                unitOfWork,
                commandGateway)
        [functionality: functionality, unitOfWork: unitOfWork]
    }

    private void executeCreatePrefix(CreateTournamentFunctionalitySagas functionality, def unitOfWork) {
        CREATE_PREFIX.each { step -> functionality.executeStepForExecutor(step, unitOfWork) }
    }

    private Map newStartQuiz(Integer quizId) {
        def unitOfWork = unitOfWorkService.createUnitOfWork(StartQuizFunctionalitySagas.simpleName)
        def functionality = new StartQuizFunctionalitySagas(
                unitOfWorkService,
                quizId,
                courseExecutionDto.aggregateId,
                studentDto.aggregateId,
                unitOfWork,
                commandGateway)
        [functionality: functionality, unitOfWork: unitOfWork]
    }

    private void executeStartQuiz(StartQuizFunctionalitySagas functionality, def unitOfWork) {
        functionality.executeStepForExecutor('getQuizStep', unitOfWork)
        functionality.executeStepForExecutor('getUserStep', unitOfWork)
        functionality.executeStepForExecutor('startQuizStep', unitOfWork)
        assert functionality.finalizeForExecutor(unitOfWork).success()
    }

    private static Throwable injectCreateTournamentFault(CreateTournamentFunctionalitySagas functionality, def unitOfWork) {
        def context = new FaultVectorBoundaryContext(
                'exploratory-attempt',
                'create-tournament-start-quiz-window',
                'create-tournament-1',
                'createTournamentStep-occurrence',
                6,
                CreateTournamentFunctionalitySagas.name,
                CreateTournamentFunctionalitySagas.simpleName,
                'createTournamentStep',
                1)
        def providerScope = FaultVectorProviderHolder.install(
                new InMemoryFaultVectorProvider([(6): FaultVectorFault.from(context)]))
        def boundaryScope = FaultVectorProviderHolder.enterBoundary(context)
        try {
            functionality.executeStepForExecutor('createTournamentStep', unitOfWork)
            throw new AssertionError('Expected assigned createTournamentStep fault')
        } catch (CompletionException failure) {
            return failure.cause
        } finally {
            boundaryScope.close()
            providerScope.close()
        }
    }

    private static class RecordingInvariantRecorder implements DynamicEvidenceRecorder {
        private final DynamicEvidenceRecorder delegate
        private final List<DynamicEvidenceEvent> invariantViolations = []

        private RecordingInvariantRecorder(DynamicEvidenceRecorder delegate) {
            this.delegate = delegate
        }

        @Override
        boolean isEnabled() {
            true
        }

        @Override
        void record(DynamicEvidenceEvent event) {
            if (event.eventKind == 'INVARIANT_VIOLATION') {
                invariantViolations.add(event)
            }
            delegate.record(event)
        }

        @Override
        void close() {
            // The recorder installed by the test/application context owns its lifecycle.
        }
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
