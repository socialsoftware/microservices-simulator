package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService

import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities.QuizFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.UpdateStudentNameFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament.CreateTournamentFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.TraceService

@DataJpaTest
class CreateTournamentFaultTest extends QuizzesSpockTest {
    public static final String UPDATED_NAME = "UpdatedName"

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService

    @Autowired
    private CourseExecutionService courseExecutionService
    @Autowired
    private TournamentService tournamentService
    @Autowired
    private CourseExecutionFactory courseExecutionFactory

    @Autowired
    private CourseExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities
    @Autowired
    private QuizFunctionalities quizFunctionalities

    @Autowired
    private EventService eventService

    @Autowired
    public TraceService traceService

    @Autowired
    private TopicService topicService

    @Autowired
    private QuizService quizService

    @Autowired
    private TournamentEventHandling tournamentEventHandling

    @Autowired
    private LocalCommandGateway commandGateway

    private CourseExecutionDto courseExecutionDto
    private UserDto userCreatorDto, userDto
    private TopicDto topicDto1, topicDto2, topicDto3
    private QuestionDto questionDto1, questionDto2, questionDto3
    private TournamentDto tournamentDto

    def unitOfWork1, unitOfWork2

    def setup() {
        given: 'load a behavior specification'
        loadBehaviorScripts()
        traceService.startRootSpan()

        and: 'a course execution'
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)

        and: 'a user to enroll in the course execution'
        userCreatorDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())

        and: 'another user to enroll in the course execution'
        userDto = createUser(USER_NAME_2, USER_USERNAME_2, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto.aggregateId)

        and: 'three topics'
        topicDto1 = createTopic(courseExecutionDto, TOPIC_NAME_1)
        topicDto2 = createTopic(courseExecutionDto, TOPIC_NAME_2)
        topicDto3 = createTopic(courseExecutionDto, TOPIC_NAME_3)

        and: 'three questions'
        questionDto1 = createQuestion(courseExecutionDto, new HashSet<>(Arrays.asList(topicDto1)), TITLE_1, CONTENT_1, OPTION_1, OPTION_2)
        questionDto2 = createQuestion(courseExecutionDto, new HashSet<>(Arrays.asList(topicDto2)), TITLE_2, CONTENT_2, OPTION_3, OPTION_4)
        questionDto3 = createQuestion(courseExecutionDto, new HashSet<>(Arrays.asList(topicDto3)), TITLE_3, CONTENT_3, OPTION_1, OPTION_3)

        and: 'a tournament '
        tournamentDto = new TournamentDto(startTime: DateHandler.toISOString(TIME_1), endTime: DateHandler.toISOString(TIME_3), numberOfQuestions: 2)
    }

    def cleanup() {
        behaviourService.cleanUpCounter()
    }

    def 'Check Quiz existence'() {
        given: 'a clear report'
        behaviourService.cleanReportFile()
        
        and: 'create unit of works for the creation of a tournament'
        def functionalityName1 = CreateTournamentFunctionalitySagas.class.getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)

        and: 'the create tournament functionality'
        def createTournamentFunctionality = new CreateTournamentFunctionalitySagas(
            tournamentService, courseExecutionService, topicService, quizService, unitOfWorkService, userCreatorDto.getAggregateId(),
             courseExecutionDto.getAggregateId(), [topicDto1.getAggregateId(), topicDto2.getAggregateId()], tournamentDto, unitOfWork1, commandGateway)
        

        when: 'execute until the step that generates the quiz'
        createTournamentFunctionality.executeUntilStep("generateQuizStep", unitOfWork1)
        then: 'check that the quiz was created'
        def quizdto = quizFunctionalities.findQuiz(11)
        quizdto != null

        when: 'we set a fault in the next step'
            createTournamentFunctionality.resumeWorkflow(unitOfWork1)
        then:'Fault is thrown'
        thrown( SimulatorException)
        when:' we check if the quiz still exists'
        def quizdto1 = quizFunctionalities.findQuiz(11)
        then:' it does not exist'
        thrown( SimulatorException)

        traceService.endRootSpan()
        traceService.spanFlush()
        behaviourService.cleanDirectory()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
