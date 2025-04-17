package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.UpdateStudentNameFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament.AddParticipantFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService

import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService

@DataJpaTest
class SimpleTest extends QuizzesSpockTest {
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
    private EventService eventService;

    @Autowired
    private TournamentEventHandling tournamentEventHandling

    @Autowired
    private BehaviourService behaviourService

    private CourseExecutionDto courseExecutionDto
    private UserDto userCreatorDto, userDto
    private TopicDto topicDto1, topicDto2, topicDto3
    private QuestionDto questionDto1, questionDto2, questionDto3
    private TournamentDto tournamentDto

    def unitOfWork1, unitOfWork2

    def setup() {
        given:
        'load behaviour directory'
        def mavenBaseDir = System.getProperty("maven.basedir", new File(".").absolutePath)
        behaviourService.LoadDir(mavenBaseDir, "groovy/" + this.class.simpleName)
        
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
        tournamentDto = createTournament(TIME_1, TIME_3, 2, userCreatorDto.getAggregateId(),  courseExecutionDto.getAggregateId(), [topicDto1.getAggregateId(),topicDto2.getAggregateId()])

        and: 'two units of work'
        def functionalityName1 = UpdateStudentNameFunctionalitySagas.class.getSimpleName()
        def functionalityName2 = AddParticipantFunctionalitySagas.class.getSimpleName()
        unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

       
        
    }

    def cleanup() {}

    def 'test' () {
        given: 'a clear report'
        behaviourService.cleanReportFile()

        and:'add participant executes the first step'
        def addParticipantFunctionality = new AddParticipantFunctionalitySagas(tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId(), unitOfWork2)
        addParticipantFunctionality.executeWorkflow(unitOfWork2);
        
        and: 'student name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userDto.getAggregateId(), updateNameDto)

        when: 'student is added to tournament update'
        addParticipantFunctionality.resumeWorkflow(unitOfWork2)

        then: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
        and: 'the name is not updated in tournament'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.getParticipants().find{it.aggregateId == userDto.aggregateId}.name == USER_NAME_2

        when: 'update name event is processed such that the participant is updated in tournament'
        tournamentEventHandling.handleUpdateStudentNameEvent();

        then: 'the name is updated in course execution'
        def courseExecutionDtoResult2 = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult2.getStudents().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
        and: 'the name is updated in tournament'
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.getParticipants().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME

        cleanup:
        behaviourService.cleanUpCounter()
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
        def addParticipantFunctionality1 = new AddParticipantFunctionalitySagas(tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId(), unitOfWork1)
        def addParticipantFunctionality2 = new AddParticipantFunctionalitySagas(tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto3.getAggregateId(), unitOfWork2)
        and: 'the first functionality reads one student'
        addParticipantFunctionality1.executeWorkflow(unitOfWork1)
        and: 'the second functionality read the other student'
        addParticipantFunctionality2.executeWorkflow(unitOfWork2)

        when: 'the first functionality ends'
        addParticipantFunctionality1.resumeWorkflow(unitOfWork1)
        then: 'the first student is a participant'
        def updatedTournament = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournament.participants.size() == 2
        updatedTournament.participants.any { it.aggregateId == userDto.getAggregateId() }

        when: 'the second functionality ends'
        addParticipantFunctionality2.resumeWorkflow(unitOfWork2)
        then: 'both participants are successfully added to the tournament'
        def updatedTournament2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournament2.participants.size() == 2
        updatedTournament2.participants.any { it.aggregateId == userDto.getAggregateId() }
        updatedTournament2.participants.any { it.aggregateId == userDto3.getAggregateId() }

        cleanup:
        behaviourService.cleanUpCounter()
    }

   @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}