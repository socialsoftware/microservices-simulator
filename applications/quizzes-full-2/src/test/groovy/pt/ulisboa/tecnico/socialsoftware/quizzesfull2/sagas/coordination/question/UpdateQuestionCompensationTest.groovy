package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.question

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
@Import(UpdateQuestionCompensationTest.LocalBeanConfiguration)
class UpdateQuestionCompensationTest extends QuizzesFull2SpockTest {

    Integer courseAggregateId
    Integer questionAggregateId

    def setup() {
        loadBehaviorScripts()
        courseAggregateId = createCourse()
        questionAggregateId = createQuestion(courseAggregateId)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updateQuestion: fault on updateQuestionStep compensates the lock acquired by getQuestionStep"() {
        // Spec: plan.md §5 Question — UpdateQuestion(questionAggregateId, ...); saga state IN_UPDATE_QUESTION
        when:
        questionFunctionalities.updateQuestion(questionAggregateId, "Merge sort complexity",
                "State the worst-case complexity of merge sort.", [])

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(questionAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the update never ran: the question still carries its pre-saga title and content'
        def reread = questionFunctionalities.getQuestionById(questionAggregateId)
        reread.title == QUESTION_TITLE
        reread.content == QUESTION_CONTENT
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
