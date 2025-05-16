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
class AddParticipantWithDelaysTest extends QuizzesSpockTest {
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
    private TournamentEventHandling tournamentEventHandling

    private CourseExecutionDto courseExecutionDto
    private UserDto userCreatorDto, userDto
    private TopicDto topicDto1, topicDto2, topicDto3
    private QuestionDto questionDto1, questionDto2, questionDto3
    private TournamentDto tournamentDto

    def unitOfWork1

    def setup() {
        given: 'load a behavior specification'
        loadBehaviorScripts()

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

        and: 'one unit of work for AddParticipantFunctionality'
        def functionalityName1 = AddParticipantFunctionalitySagas.class.getSimpleName()
        unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
    }

    def cleanup() {
        behaviourService.cleanUpCounter()
    }

    def 'add one participant with a delay to tournament'() {
        given: 'a clear report'
        behaviourService.cleanReportFile()

        behaviourService.generateTestBehaviour("input.txt")

        and: 'one functionality to add a participant'
        def addParticipantFunctionality1 = new AddParticipantFunctionalitySagas(
            tournamentService, courseExecutionService, unitOfWorkService,
            tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(),
            userDto.getAggregateId(), unitOfWork1
        )

        when: 'adding a participant measure the time taken to execute the workflow'
        def start = System.currentTimeMillis()
        addParticipantFunctionality1.executeWorkflow(unitOfWork1)
        def end = System.currentTimeMillis()
        def duration = end - start

        and: 'get the defined delay'
        def definedDelay = addParticipantFunctionality1.getWorkflowTotalDelay()

        then: 'check number of participants accordingly and comparing the time taken with the expected delay'
        def updatedTournament = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournament.participants.size() == 1
        updatedTournament.participants.any { it.aggregateId == userDto.getAggregateId() }

        and: 'the execution duration of AddParticipantFunctionality is bigger than the defined delay'
        duration > definedDelay
    }


    def 'concurrent: add two participants to tournament, where one of the functionalities has a delay'() {
        given: 'another user'
        def userDto3 = createUser(USER_NAME_3, USER_USERNAME_3, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto3.aggregateId)

        and: 'create another unit of work for concurrent addition of participants'
        def functionalityName2 = AddParticipantFunctionalitySagas.class.getSimpleName()
        def unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

        and: 'two functionalities to add participants'
        def addParticipantFunctionality1 = new AddParticipantFunctionalitySagas(
            tournamentService, courseExecutionService, unitOfWorkService,
            tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(),
            userDto.getAggregateId(), unitOfWork1
        )
        def addParticipantFunctionality2 = new AddParticipantFunctionalitySagas(
            tournamentService, courseExecutionService, unitOfWorkService,
            tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(),
            userDto3.getAggregateId(), unitOfWork2
        )

        when: 'executing both workflows, capture the time taken to execute each functionality\'s workflow'
        def start1 = System.currentTimeMillis()
        addParticipantFunctionality1.executeWorkflow(unitOfWork1)
        def end1 = System.currentTimeMillis()
        def duration1 = end1 - start1
        def start2 = System.currentTimeMillis()
        addParticipantFunctionality2.executeWorkflow(unitOfWork2)
        def end2 = System.currentTimeMillis()
        def duration2 = end2 - start2

        and: 'get the defined delay of the first functionality'
        def definedDelayFunc1 = addParticipantFunctionality1.getWorkflowTotalDelay()

        then: 'check number of participants accordingly and comparing the time taken with the expected delay'
        def updatedTournament = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournament.participants.size() == 2
        updatedTournament.participants.any { it.aggregateId == userDto.getAggregateId() }

        and: 'the execution duration of AddParticipantFunctionality1 is bigger than the execution duration of AddParticipantFunctionality2 + defined delay for func1'
        duration1 > duration2 + definedDelayFunc1

        cleanup: 'remove all generated artifacts after test execution'
        behaviourService.cleanDirectory()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
