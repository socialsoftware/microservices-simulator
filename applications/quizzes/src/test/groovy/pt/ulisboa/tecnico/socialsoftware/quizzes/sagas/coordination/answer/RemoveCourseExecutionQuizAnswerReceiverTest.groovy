package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.answer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventRepository
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteCourseExecutionEvent
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.functionalities.QuizAnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

@DataJpaTest
class RemoveCourseExecutionQuizAnswerReceiverTest extends QuizzesSpockTest {

    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private QuizFunctionalities quizFunctionalities
    @Autowired
    private QuizAnswerFunctionalities quizAnswerFunctionalities
    @Autowired
    private QuizAnswerRepository quizAnswerRepository
    @Autowired
    private EventRepository eventRepository

    private CourseExecutionDto courseExecutionDto
    private UserDto userDto

    def setup() {
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE,
                COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto.aggregateId)
        TopicDto topicDto = createTopic(courseExecutionDto, TOPIC_NAME_1)
        QuestionDto questionDto = createQuestion(courseExecutionDto, new HashSet<>(Arrays.asList(topicDto)),
                TITLE_1, CONTENT_1, OPTION_1, OPTION_2)

        def quizDtoInput = new QuizDto()
        quizDtoInput.title = TITLE_1
        quizDtoInput.availableDate = DateHandler.toISOString(TIME_1)
        quizDtoInput.conclusionDate = DateHandler.toISOString(TIME_2)
        quizDtoInput.resultsDate = DateHandler.toISOString(TIME_3)
        quizDtoInput.questionDtos = Arrays.asList(questionDto)
        QuizDto quizDto = quizFunctionalities.createQuiz(courseExecutionDto.aggregateId, quizDtoInput)
        quizAnswerFunctionalities.startQuiz(quizDto.aggregateId, courseExecutionDto.aggregateId, userDto.aggregateId)

        createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, ACRONYM_1,
                COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)
        courseExecutionFunctionalities.removeStudentFromCourseExecution(
                courseExecutionDto.aggregateId, userDto.aggregateId)
    }

    def cleanup() {}

    def "RemoveCourseExecution event route has an eligible quiz answer receiver"() {
        when:
        courseExecutionFunctionalities.removeCourseExecution(courseExecutionDto.aggregateId)

        then:
        def event = eventRepository.findAll().find {
            it instanceof DeleteCourseExecutionEvent &&
                    it.publisherAggregateId == courseExecutionDto.aggregateId
        }
        event != null
        quizAnswerRepository.findLatestQuizAnswer().orElseThrow().eventSubscriptions.any {
            it.subscribesEvent(event)
        }
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
