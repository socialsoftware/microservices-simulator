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
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.utils.DatabaseService

@DataJpaTest
class CleanDatabase extends QuizzesSpockTest {
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
    private DatabaseService databaseService;

    @Autowired
    private TournamentEventHandling tournamentEventHandling

    private CourseExecutionDto courseExecutionDto
    private UserDto userCreatorDto, userDto
    private TopicDto topicDto1, topicDto2, topicDto3
    private QuestionDto questionDto1, questionDto2, questionDto3
    private TournamentDto tournamentDto

    def unitOfWork1, unitOfWork2

    def setup() {
        given: 'a course execution'
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

    def cleanup() {
    }

    // def 'sequential - add creator: update; add: fails because when creator is added tournament have not process the event yet' () {

    //     given: 'creator name is updated'
    //     def updateNameDto = new UserDto()
    //     updateNameDto.setName(UPDATED_NAME)
    //     courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto)

    //     when: 'event is finally processed it updates the creator name'
    //     tournamentEventHandling.handleUpdateStudentNameEvent()
    //     then: ''
    //     def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
    //     courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME

    //     when: ''
    //     databaseService.showDatabaseInfo()
    //     println "Clean database after updating creator name:"
    //     databaseService.cleanDatabase()

    //     then: "if a new user is created"
    //     createUser(USER_NAME_3, USER_USERNAME_3, STUDENT_ROLE)
    //     println "Clean database after creating a new user:"
    //     databaseService.showDatabaseInfo()

    //     then: 'the database is clean'
    //     def courseExecutionDtoResult1 = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
    //     courseExecutionDtoResult1.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
    // }

    def 'clean database tables' () {

        given: 'a database with users, topics, questions and a tournament'
        databaseService.showDatabaseInfo()


        when: 'database is cleaned'
        println "Clean database"
        databaseService.cleanDatabase()

        and: "a new user is created"
        createUser(USER_NAME_3, USER_USERNAME_3, STUDENT_ROLE)

        then: 'the user table has the new user and all other tables are empty'
        println "Clean database after creating a new user:"
        databaseService.showDatabaseInfo()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}