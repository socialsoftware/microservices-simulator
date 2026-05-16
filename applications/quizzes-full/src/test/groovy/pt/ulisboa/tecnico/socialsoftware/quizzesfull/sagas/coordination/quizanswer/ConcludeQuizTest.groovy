package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.quizanswer

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class ConcludeQuizTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    Integer courseId
    Integer userId
    Integer executionId
    Integer topicId
    Integer questionId
    Integer quizId
    Integer quizAnswerId

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

        def quizAnswer = createQuizAnswer(quizId, userId)
        quizAnswerId = quizAnswer.aggregateId
    }

    def "concludeQuiz: success"() {
        when:
        quizAnswerFunctionalities.concludeQuiz(quizAnswerId)

        then:
        def uow = unitOfWorkService.createUnitOfWork("check")
        def dto = quizAnswerService.getQuizAnswerById(quizAnswerId, uow)
        dto.completed == true
    }
}
