package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.quizanswer

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.UNIQUE_QUIZ_ANSWER_PER_STUDENT

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateQuizAnswerTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    Integer courseId
    Integer userId
    Integer executionId
    Integer topicId
    Integer questionId
    Integer quizId

    def setup() {
        def course = createCourse("Software Engineering", "TECNICO")
        courseId = course.aggregateId

        def user = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        userId = user.aggregateId

        def execution = createExecution(courseId, "SE2024", "1st Semester 2024")
        executionId = execution.aggregateId

        executionFunctionalities.enrollStudentInExecution(executionId, userId)

        def topic = createTopic(courseId, "Topic A")
        topicId = topic.aggregateId

        def question = createQuestion(courseId, [topicId], "Q1", "Content")
        questionId = question.aggregateId

        def quiz = createQuiz(executionId, [questionId])
        quizId = quiz.aggregateId
    }

    def "createQuizAnswer: success"() {
        when:
        def result = createQuizAnswer(quizId, userId)

        then:
        result.aggregateId != null
        result.quizAggregateId == quizId
        result.userAggregateId == userId
        result.completed == false
    }

    def "createQuizAnswer: UNIQUE_QUIZ_ANSWER_PER_STUDENT violation"() {
        given:
        createQuizAnswer(quizId, userId)

        when:
        createQuizAnswer(quizId, userId)

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == UNIQUE_QUIZ_ANSWER_PER_STUDENT
    }
}
