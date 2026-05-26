package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.quizanswer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.sagas.states.QuizAnswerSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.sagas.AnswerQuestionFunctionalitySagas

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.QUESTION_ALREADY_ANSWERED

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class AnswerQuestionTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

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

    def "answerQuestion: success"() {
        when:
        quizAnswerFunctionalities.answerQuestion(quizAnswerId, questionId, 1, 30)

        then: 'the answered question is reflected on the quiz answer state'
        def uow = unitOfWorkService.createUnitOfWork("check")
        def dto = quizAnswerService.getQuizAnswerById(quizAnswerId, uow)
        dto.questionAnswerIds.size() == 1
        dto.questionAnswerIds.contains(questionId)
        dto.completed == false
    }

    def "answerQuestion: QUESTION_ALREADY_ANSWERED violation"() {
        given:
        quizAnswerFunctionalities.answerQuestion(quizAnswerId, questionId, 1, 30)

        when:
        quizAnswerFunctionalities.answerQuestion(quizAnswerId, questionId, 1, 30)

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == QUESTION_ALREADY_ANSWERED
    }

    def "answerQuestion: getQuizAnswerStep acquires IN_ANSWER_QUESTION semantic lock"() {
        given: 'an answerQuestion workflow pauses after getQuizAnswerStep has acquired IN_ANSWER_QUESTION lock'
        def uow = unitOfWorkService.createUnitOfWork("answerQuestion")
        def func = new AnswerQuestionFunctionalitySagas(
                unitOfWorkService, quizAnswerId, questionId, 1, 30, uow, commandGateway)
        func.executeUntilStep("getQuizAnswerStep", uow)

        expect: 'quiz answer saga state is IN_ANSWER_QUESTION'
        sagaStateOf(quizAnswerId) == QuizAnswerSagaState.IN_ANSWER_QUESTION

        when: 'workflow resumes and completes'
        func.resumeWorkflow(uow)

        then:
        noExceptionThrown()
    }
}
