package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.quizanswer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.sagas.states.QuizAnswerSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.sagas.AnswerQuestionFunctionalitySagas

@DataJpaTest
@Transactional
@Import(AnswerQuestionTest.LocalBeanConfiguration)
class AnswerQuestionTest extends QuizzesFull2SpockTest {

    @Autowired
    LocalCommandGateway commandGateway

    Integer questionAggregateId
    Integer quizAnswerAggregateId

    def setup() {
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        questionAggregateId = createQuestionWithOptions(courseAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId, QUIZ_TITLE, QUIZ_AVAILABLE_DATE,
                QUIZ_CONCLUSION_DATE, QUIZ_RESULTS_DATE, QUIZ_TYPE, [questionAggregateId])
        quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)
    }

    def "answerQuestion: success"() {
        // Spec: plan.md §7 QuizAnswer — AnswerQuestion(quizAnswerAggregateId, questionAggregateId,
        // optionSequenceChoice, optionKey, timeTaken)
        when:
        quizAnswerFunctionalities.answerQuestion(quizAnswerAggregateId, questionAggregateId,
                QUESTION_ANSWER_OPTION_SEQUENCE_CHOICE, QUESTION_ANSWER_CORRECT_OPTION_KEY,
                QUESTION_ANSWER_TIME_TAKEN)

        then: 'orchestration outcome only — persistence is asserted in T2'
        sagaStateOf(quizAnswerAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "answerQuestion: getQuizAnswerStep acquires IN_ANSWER_QUESTION semantic lock"() {
        // Spec: plan.md §7 QuizAnswer — saga state IN_ANSWER_QUESTION acquired by the primary lock step
        given:
        def uow = unitOfWorkService.createUnitOfWork("answerQuestion")
        def func = new AnswerQuestionFunctionalitySagas(unitOfWorkService, quizAnswerAggregateId,
                questionAggregateId, QUESTION_ANSWER_OPTION_SEQUENCE_CHOICE,
                QUESTION_ANSWER_CORRECT_OPTION_KEY, QUESTION_ANSWER_TIME_TAKEN, uow, commandGateway)
        func.executeUntilStep("getQuizAnswerStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_ANSWER_QUESTION'
        sagaStateOf(quizAnswerAggregateId) == QuizAnswerSagaState.IN_ANSWER_QUESTION

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
