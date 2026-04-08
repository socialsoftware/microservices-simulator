package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.answer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventRepository
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteCourseExecutionEvent
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.InvalidateQuizEvent
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.functionalities.QuizAnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.QuizAnswerEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

@DataJpaTest
class QuizAnswerEventHandlingTest extends QuizzesSpockTest {

    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private QuizFunctionalities quizFunctionalities
    @Autowired
    private QuizAnswerFunctionalities quizAnswerFunctionalities
    @Autowired
    private QuizAnswerEventHandling quizAnswerEventHandling
    @Autowired
    private QuizAnswerRepository quizAnswerRepository
    @Autowired
    private EventRepository eventRepository

    private CourseExecutionDto courseExecutionDto
    private UserDto userDto
    private TopicDto topicDto
    private QuestionDto questionDto
    private QuizDto quizDto

    def setup() {
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userDto.getAggregateId())
        topicDto = createTopic(courseExecutionDto, TOPIC_NAME_1)
        questionDto = createQuestion(courseExecutionDto, new HashSet<>(Arrays.asList(topicDto)), TITLE_1, CONTENT_1, OPTION_1, OPTION_2)

        def quizDtoInput = new QuizDto()
        quizDtoInput.setTitle(TITLE_1)
        quizDtoInput.setAvailableDate(DateHandler.toISOString(TIME_1))
        quizDtoInput.setConclusionDate(DateHandler.toISOString(TIME_2))
        quizDtoInput.setResultsDate(DateHandler.toISOString(TIME_3))
        quizDtoInput.setQuestionDtos(Arrays.asList(questionDto))
        quizDto = quizFunctionalities.createQuiz(courseExecutionDto.getAggregateId(), quizDtoInput)

        quizAnswerFunctionalities.startQuiz(quizDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())
    }

    def cleanup() {}

    def "AnonymizeStudentEvent updates the student name in the quiz answer"() {
        when:
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.getAggregateId(), userDto.getAggregateId())
        quizAnswerEventHandling.handleAnonymizeStudentEvents()

        then:
        quizAnswerRepository.findLatestQuizAnswer().get().getStudent().getName() == ANONYMOUS
    }

    def "InvalidateQuizEvent removes the quiz answer"() {
        given: "an InvalidateQuizEvent published for the quiz"
        def quizVersion = quizAnswerRepository.findLatestQuizAnswer().get().getQuiz().getQuizVersion()
        def event = new InvalidateQuizEvent(quizDto.getAggregateId())
        event.setPublisherAggregateVersion(quizVersion + 1)
        event.setPublished(true)
        eventRepository.save(event)

        when:
        quizAnswerEventHandling.handleInvalidateQuizEvents()

        then:
        quizAnswerRepository.findLatestQuizAnswer().get().getState() == Aggregate.AggregateState.DELETED
    }

    def "DeleteCourseExecutionEvent removes the quiz answer"() {
        given: "a DeleteCourseExecutionEvent published for the course execution"
        def executionVersion = quizAnswerRepository.findLatestQuizAnswer().get().getAnswerCourseExecution().getCourseExecutionVersion()
        def event = new DeleteCourseExecutionEvent(courseExecutionDto.getAggregateId())
        event.setPublisherAggregateVersion(executionVersion + 1)
        event.setPublished(true)
        eventRepository.save(event)

        when:
        quizAnswerEventHandling.handleDeleteCourseExecutionEvents()

        then:
        quizAnswerRepository.findLatestQuizAnswer().get().getState() == Aggregate.AggregateState.DELETED
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
