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
@Import(DeleteQuestionCompensationTest.LocalBeanConfiguration)
class DeleteQuestionCompensationTest extends QuizzesFull2SpockTest {

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

    def "deleteQuestion: fault on deleteQuestionStep compensates the lock acquired by getQuestionStep"() {
        // Spec: plan.md §5 Question — DeleteQuestion(questionAggregateId); saga state IN_DELETE_QUESTION
        when:
        questionFunctionalities.deleteQuestion(questionAggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(questionAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the soft-delete never ran: the question is still readable in its pre-saga state'
        def reread = questionFunctionalities.getQuestionById(questionAggregateId)
        reread.title == QUESTION_TITLE
        reread.courseAggregateId == courseAggregateId
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
