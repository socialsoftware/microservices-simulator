package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.quizanswer

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

@DataJpaTest
@Transactional
@Import(AnswerQuestionCompensationTest.LocalBeanConfiguration)
class AnswerQuestionCompensationTest extends QuizzesFull2SpockTest {

    Integer questionAggregateId
    Integer quizAnswerAggregateId

    def setup() {
        loadBehaviorScripts()
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        questionAggregateId = createQuestionWithOptions(courseAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId, QUIZ_TITLE, QUIZ_AVAILABLE_DATE,
                QUIZ_CONCLUSION_DATE, QUIZ_RESULTS_DATE, QUIZ_TYPE, [questionAggregateId])
        quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "answerQuestion: fault on answerQuestionStep compensates the lock acquired by getQuizAnswerStep"() {
        // Spec: plan.md §7 QuizAnswer — AnswerQuestion(quizAnswerAggregateId, ...); saga state
        // IN_ANSWER_QUESTION
        when:
        quizAnswerFunctionalities.answerQuestion(quizAnswerAggregateId, questionAggregateId,
                QUESTION_ANSWER_OPTION_SEQUENCE_CHOICE, QUESTION_ANSWER_CORRECT_OPTION_KEY,
                QUESTION_ANSWER_TIME_TAKEN)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(quizAnswerAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the answer never ran: the seeded QuestionAnswer still carries no chosen option'
        def reread = quizAnswerFunctionalities.getQuizAnswerById(quizAnswerAggregateId)
        reread.questionAnswers[0].optionKey == null
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
