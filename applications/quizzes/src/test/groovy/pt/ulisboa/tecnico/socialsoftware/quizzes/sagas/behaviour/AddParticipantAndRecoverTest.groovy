package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.TraceService
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.UpdateStudentNameFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

@DataJpaTest
class AddParticipantAndRecoverTest extends QuizzesSpockTest {
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
    private EventService eventService

    @Autowired
    public TraceService traceService

    @Autowired
    private TournamentEventHandling tournamentEventHandling

    @Autowired
    private LocalCommandGateway commandGateway;

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

        and: 'a tournament where the first user is the creator'
        tournamentDto = createTournament(TIME_1, TIME_3, 2, userCreatorDto.getAggregateId(), courseExecutionDto.getAggregateId(), [topicDto1.getAggregateId(), topicDto2.getAggregateId()])

        and: 'two units of Functionalities'
        def functionalityName1 = UpdateStudentNameFunctionalitySagas.class.getSimpleName()
        def functionalityName2 = AddParticipantFunctionalitySagas.class.getSimpleName()

        and: 'two unit of works'
        unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)
    }

    def cleanup() {
        behaviourService.cleanUpCounter()
    }

    def 'add one participant to tournament'() {
        given: 'a clear report'
        behaviourService.cleanReportFile()
        
        and: 'create unit of works for concurrent addition of participants'
        def functionalityName1 = AddParticipantFunctionalitySagas.class.getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)

        and: 'one functionalities to add participants'
        def addParticipantFunctionality1 = new AddParticipantFunctionalitySagas(
                unitOfWorkService,
            tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(),
            userDto.getAggregateId(), unitOfWork1, commandGateway
        )

        when: 'executing both workflows, capturing exceptions if any'
        boolean exceptionThrown = false
        try {
            addParticipantFunctionality1.executeWorkflow(unitOfWork1)
        } catch (Exception e) {
            exceptionThrown = true
        }
        then: 'check number of participants accordingly'
        def updatedTournament = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        if (exceptionThrown) {
            println "\u001B[31mEntered the exceptionThrown branch\u001B[0m"
            assert updatedTournament.participants.size() == 0
        } else {
            assert updatedTournament.participants.size() == 1
        }
        traceService.endRootSpan()
        traceService.spanFlush()
    }

    def 'concurrent: add two participants to tournament'() {
        given: 'another user'
        def userDto3 = createUser(USER_NAME_3, USER_USERNAME_3, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto3.aggregateId)

        and: 'create unit of works for concurrent addition of participants'
        def functionalityName1 = AddParticipantFunctionalitySagas.class.getSimpleName()
        def functionalityName2 = AddParticipantFunctionalitySagas.class.getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        def unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

        and: 'two functionalities to add participants'
        def addParticipantFunctionality1 = new AddParticipantFunctionalitySagas(
                unitOfWorkService,
            tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(),
            userDto.getAggregateId(), unitOfWork1, commandGateway
        )
        def addParticipantFunctionality2 = new AddParticipantFunctionalitySagas(
                unitOfWorkService,
            tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(),
            userDto3.getAggregateId(), unitOfWork2, commandGateway
        )

        when: 'executing both workflows, capturing exceptions if any'
        Integer exceptionThrown = 0
        try {
            addParticipantFunctionality1.executeWorkflow(unitOfWork1)
        } catch (Exception e) {
            exceptionThrown += 1
        }

        try {
            addParticipantFunctionality2.executeWorkflow(unitOfWork2)
        } catch (Exception e) {
            exceptionThrown += 1
        }

        then: 'check number of participants accordingly'
        def updatedTournament = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        if (exceptionThrown == 1) {
            println "\u001B[31mEntered the exceptionThrown branch. Just 1 participant added\u001B[0m"
            assert updatedTournament.participants.size() == 1
        } else if (exceptionThrown == 2) {
            println "\u001B[31mEntered the exceptionThrown branch. No participants added\u001B[0m"
            assert updatedTournament.participants.size() == 0
        } else {
            assert updatedTournament.participants.size() == 2
        }
        traceService.endRootSpan()
        // traceService.spanFlush()
        behaviourService.cleanDirectory()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
