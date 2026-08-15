package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.execution

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
@Import(UpdateExecutionCompensationTest.LocalBeanConfiguration)
class UpdateExecutionCompensationTest extends QuizzesFull2SpockTest {

    public static final String UPDATED_EXECUTION_ACRONYM = "SE-02"
    public static final String UPDATED_EXECUTION_ACADEMIC_TERM = "2026/2027"

    Integer courseAggregateId
    Integer executionAggregateId

    def setup() {
        loadBehaviorScripts()
        courseAggregateId = createCourse()
        executionAggregateId = createExecution(courseAggregateId)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updateExecution: fault on updateExecutionStep compensates the lock acquired by getExecutionStep"() {
        // Spec: plan.md §4 Execution — UpdateExecution(executionAggregateId, acronym, academicTerm); saga state IN_UPDATE_EXECUTION
        when:
        executionFunctionalities.updateExecution(executionAggregateId, UPDATED_EXECUTION_ACRONYM,
                UPDATED_EXECUTION_ACADEMIC_TERM)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(executionAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the update never ran: the execution still carries its pre-saga acronym and academic term'
        def reread = executionFunctionalities.getExecutionById(executionAggregateId)
        reread.acronym == EXECUTION_ACRONYM
        reread.academicTerm == EXECUTION_ACADEMIC_TERM
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
