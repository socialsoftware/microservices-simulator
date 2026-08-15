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
@Import(ConcludeQuizCompensationTest.LocalBeanConfiguration)
class ConcludeQuizCompensationTest extends QuizzesFull2SpockTest {

    Integer quizAnswerAggregateId

    def setup() {
        loadBehaviorScripts()
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)
        quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "concludeQuiz: fault on concludeQuizStep compensates the lock acquired by getQuizAnswerStep"() {
        // Spec: plan.md §7 QuizAnswer — ConcludeQuiz(quizAnswerAggregateId); saga state IN_CONCLUDE_QUIZ
        when:
        quizAnswerFunctionalities.concludeQuiz(quizAnswerAggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(quizAnswerAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the conclusion never ran: the quiz answer is still open'
        def reread = quizAnswerFunctionalities.getQuizAnswerById(quizAnswerAggregateId)
        !reread.completed
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
