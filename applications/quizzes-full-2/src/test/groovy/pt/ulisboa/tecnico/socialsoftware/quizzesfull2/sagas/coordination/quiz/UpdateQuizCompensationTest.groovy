package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.quiz

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
@Import(UpdateQuizCompensationTest.LocalBeanConfiguration)
class UpdateQuizCompensationTest extends QuizzesFull2SpockTest {

    public static final String UPDATED_QUIZ_TITLE = "Sorting and searching quiz"

    Integer courseAggregateId
    Integer executionAggregateId
    Integer quizAggregateId

    def setup() {
        loadBehaviorScripts()
        courseAggregateId = createCourse()
        executionAggregateId = createExecution(courseAggregateId)
        quizAggregateId = createQuiz(executionAggregateId)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updateQuiz: fault on updateQuizStep compensates the lock acquired by getQuizStep"() {
        // Spec: plan.md §6 Quiz — UpdateQuiz(quizAggregateId, ...); saga state IN_UPDATE_QUIZ
        when:
        quizFunctionalities.updateQuiz(quizAggregateId, UPDATED_QUIZ_TITLE, QUIZ_AVAILABLE_DATE,
                QUIZ_CONCLUSION_DATE, QUIZ_RESULTS_DATE, [])

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(quizAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the update never ran: the quiz still carries its pre-saga title'
        def reread = quizFunctionalities.getQuizById(quizAggregateId)
        reread.title == QUIZ_TITLE
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
