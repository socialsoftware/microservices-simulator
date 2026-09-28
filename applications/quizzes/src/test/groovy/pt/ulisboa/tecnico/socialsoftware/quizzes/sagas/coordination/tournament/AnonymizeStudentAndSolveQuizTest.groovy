package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventRepository
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateStudentNameEvent
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteCourseExecutionEvent
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DisenrollStudentFromCourseExecutionEvent
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.notification.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

@DataJpaTest
class AnonymizeStudentAndSolveQuizTest extends QuizzesSpockTest {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService

    @Autowired
    private QuizAnswerService quizAnswerService

    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities

    @Autowired
    private TournamentEventHandling tournamentEventHandling
    @Autowired
    private TournamentRepository tournamentRepository
    @Autowired
    private EventRepository eventRepository

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

        and: 'the solving user is a tournament participant'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())
    }

    def cleanup() {

    }

    def 'sequential solve quiz and anonymize user'() {
        
        given: 'a quiz is solved for a user'
        tournamentFunctionalities.solveQuiz(tournamentDto.aggregateId, userDto.getAggregateId())

        when: 'the user is anonymized after starting the quiz'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.getAggregateId(), userDto.getAggregateId())
        tournamentEventHandling.handleAnonymizeStudentEvents()

        then: 'the user is anonymized in the course execution'
        def courseExecutionResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionResult.getStudents().find{ it.aggregateId == userDto.getAggregateId() }.name == ANONYMOUS

        and: 'the quiz is still started for the anonymized user'
        def unitOfWork = unitOfWorkService.createUnitOfWork("getQuizAnswerDtoByQuizIdAndUserId")
        def quizAnswerResult = quizAnswerService.getQuizAnswerDtoByQuizIdAndUserId(tournamentDto.quiz.aggregateId, userDto.getAggregateId(), unitOfWork)
        quizAnswerResult.getStudentName() == userDto.getName()
    }

    def 'UpdateStudentName event route has an eligible tournament receiver'() {
        given:
        def completeUserDto = new UserDto()
        completeUserDto.setName(USER_NAME_3)

        when:
        courseExecutionFunctionalities.updateStudentName(
                courseExecutionDto.aggregateId, userDto.aggregateId, completeUserDto)

        then:
        def event = eventRepository.findAll().find {
            it instanceof UpdateStudentNameEvent &&
                    it.publisherAggregateId == courseExecutionDto.aggregateId &&
                    it.studentAggregateId == userDto.aggregateId
        }
        event != null
        tournamentRepository.findLastAggregateVersion(tournamentDto.aggregateId)
                .orElseThrow().eventSubscriptions.any {
            it.subscribesEvent(event)
        }

        when: 'the name update reaches the existing Tournament participant'
        tournamentEventHandling.handleUpdateStudentNameEvent()

        then: 'the public query returns the propagated name'
        def result = tournamentFunctionalities.findTournament(tournamentDto.aggregateId)
        result.participants.find { it.aggregateId == userDto.aggregateId }.name == USER_NAME_3
    }

    def 'DeleteCourseExecution event reaches an existing tournament'() {
        given: 'another execution keeps the Course valid after removing one with content'
        createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, ACRONYM_1,
                COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)
        and: 'students are removed before deleting their execution, without delivering their events yet'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(
                courseExecutionDto.aggregateId, userDto.aggregateId)
        courseExecutionFunctionalities.removeStudentFromCourseExecution(
                courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        def before = tournamentRepository.findLastAggregateVersion(tournamentDto.aggregateId).orElseThrow()
        assert before.state == Aggregate.AggregateState.ACTIVE

        when: 'the real operation publishes the deletion event'
        courseExecutionFunctionalities.removeCourseExecution(courseExecutionDto.aggregateId)

        then: 'the still-active Tournament subscribes to that persisted event'
        def event = eventRepository.findAll().find {
            it instanceof DeleteCourseExecutionEvent &&
                    it.publisherAggregateId == courseExecutionDto.aggregateId
        }
        event != null
        event.published
        tournamentRepository.findLastAggregateVersion(tournamentDto.aggregateId)
                .orElseThrow().eventSubscriptions.any { it.subscribesEvent(event) }

        when: 'the application handler delivers it'
        tournamentEventHandling.handleDeleteCourseExecutionEvents()

        then: 'the latest persisted Tournament is inactive'
        def after = tournamentRepository.findAll()
                .findAll { it.aggregateId == tournamentDto.aggregateId }.max { it.version }
        after.state == Aggregate.AggregateState.INACTIVE
        after.version > before.version
        !tournamentRepository.findLastAggregateVersion(tournamentDto.aggregateId).present
    }

    def 'DisenrollStudent event reaches an existing tournament participant'() {
        given:
        def before = tournamentRepository.findLastAggregateVersion(tournamentDto.aggregateId).orElseThrow()
        assert before.tournamentParticipants.find { it.participantAggregateId == userDto.aggregateId }.state ==
                Aggregate.AggregateState.ACTIVE

        when: 'the real operation removes the student and publishes the event'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(
                courseExecutionDto.aggregateId, userDto.aggregateId)

        then: 'the existing Tournament is eligible for this student and execution'
        def event = eventRepository.findAll().find {
            it instanceof DisenrollStudentFromCourseExecutionEvent &&
                    it.publisherAggregateId == courseExecutionDto.aggregateId &&
                    it.studentAggregateId == userDto.aggregateId
        }
        event != null
        event.published
        tournamentRepository.findLastAggregateVersion(tournamentDto.aggregateId)
                .orElseThrow().eventSubscriptions.any { it.subscribesEvent(event) }

        when: 'the application handler delivers it'
        tournamentEventHandling.handleUnenrollStudentFromCourseExecutionEvents()

        then: 'the latest persisted participant is deleted and the event version is recorded'
        def after = tournamentRepository.findLastAggregateVersion(tournamentDto.aggregateId).orElseThrow()
        after.tournamentParticipants.find { it.participantAggregateId == userDto.aggregateId }.state ==
                Aggregate.AggregateState.DELETED
        after.tournamentCourseExecution.courseExecutionVersion == event.publisherAggregateVersion
        after.version > before.version
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
