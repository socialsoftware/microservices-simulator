package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.UserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.AddParticipantFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService


@DataJpaTest
class AddParticipantTest extends QuizzesSpockTest {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService

    @Autowired
    private CourseExecutionService courseExecutionService
    @Autowired
    private TournamentService tournamentService

    @Autowired
    private CourseExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities
    @Autowired
    private UserFunctionalities userFunctionalities

    @Autowired
    private EventService eventService;

    @Autowired
    private TournamentEventHandling tournamentEventHandling

    private CourseExecutionDto courseExecutionDto
    private UserDto userCreatorDto, userDto
    private TopicDto topicDto1, topicDto2, topicDto3
    private QuestionDto questionDto1, questionDto2, questionDto3
    private TournamentDto tournamentDto

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
    }

    def cleanup() {

    }

    // TODO reviw this
    def 'concurrent add two participants to tournament'() {
        given: 'two new users'
        def userDto1 = new UserDto()
        userDto1.setName('User1')
        userDto1.setUsername('Username1')
        userDto1.setRole('STUDENT')
        userDto1 = userFunctionalities.createUser(userDto1)
        userFunctionalities.activateUser(userDto1.getAggregateId())
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userDto1.getAggregateId())

        def userDto2 = new UserDto()
        userDto2.setName('User2')
        userDto2.setUsername('Username2')
        userDto2.setRole('STUDENT')
        userDto2 = userFunctionalities.createUser(userDto2)
        userFunctionalities.activateUser(userDto2.getAggregateId())
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userDto2.getAggregateId())

        and: 'create unit of works for concurrent addition of participants'
        def functionalityName1 = AddParticipantFunctionalitySagas.class.getSimpleName()
        def functionalityName2 = AddParticipantFunctionalitySagas.class.getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        def unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

        def addParticipantFunctionality1 = new AddParticipantFunctionalitySagas(eventService, tournamentEventHandling, tournamentService, 
                courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto1.getAggregateId(), unitOfWork1)
        def addParticipantFunctionality2 = new AddParticipantFunctionalitySagas(eventService, tournamentEventHandling, tournamentService, 
                courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto2.getAggregateId(), unitOfWork2)

        when: 
        addParticipantFunctionality1.executeUntilStep("getUserStep", unitOfWork1)
        addParticipantFunctionality2.executeWorkflow(unitOfWork2)

        /*
        then: 'tournament is locked'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'add finishes and add participant tries again'
        addParticipantFunctionality1.resumeWorkflow(unitOfWork1) 
        def unitOfWork3 = unitOfWorkService.createUnitOfWork(functionalityName2)
        def addParticipantFunctionality3 = new AddParticipantFunctionalitySagas(eventService, tournamentEventHandling, tournamentService, 
                courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto2.getAggregateId(), unitOfWork3)
        addParticipantFunctionality3.executeWorkflow(unitOfWork3) 
        */
        addParticipantFunctionality1.resumeWorkflow(unitOfWork1)


        then: 'both participants should be successfully added to the tournament'
        def updatedTournament = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournament.participants.size() == 2
        updatedTournament.participants.any { it.aggregateId == userDto1.getAggregateId() }
        updatedTournament.participants.any { it.aggregateId == userDto2.getAggregateId() }
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}