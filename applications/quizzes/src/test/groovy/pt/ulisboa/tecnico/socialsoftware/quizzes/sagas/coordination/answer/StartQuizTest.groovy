package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.answer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.functionalities.QuizAnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

@DataJpaTest
class StartQuizTest extends QuizzesSpockTest {

    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private QuizFunctionalities quizFunctionalities
    @Autowired
    private QuizAnswerFunctionalities quizAnswerFunctionalities

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
    }

    def cleanup() {}

    def "student can start a quiz they have not started before"() {
        when:
        quizAnswerFunctionalities.startQuiz(quizDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        then:
        noExceptionThrown()
    }

    def "student cannot start a quiz they have already started — UNIQUE_QUIZ_ANSWER_PER_STUDENT"() {
        given: "the student starts the quiz once"
        quizAnswerFunctionalities.startQuiz(quizDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        when: "the student tries to start the same quiz again"
        quizAnswerFunctionalities.startQuiz(quizDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        then: "the UNIQUE_QUIZ_ANSWER_PER_STUDENT invariant is violated"
        thrown(QuizzesException)
    }

    def "a different student can start the same quiz independently"() {
        given: "a second student enrolled in the same course execution"
        def userDto2 = createUser(USER_NAME_2, USER_USERNAME_2, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userDto2.getAggregateId())

        and: "the first student starts the quiz"
        quizAnswerFunctionalities.startQuiz(quizDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        when: "the second student starts the same quiz"
        quizAnswerFunctionalities.startQuiz(quizDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto2.getAggregateId())

        then: "the second student succeeds — guard only blocks the same student"
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
