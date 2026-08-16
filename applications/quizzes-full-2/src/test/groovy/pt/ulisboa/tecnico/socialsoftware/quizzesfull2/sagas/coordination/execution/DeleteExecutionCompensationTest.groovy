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
@Import(DeleteExecutionCompensationTest.LocalBeanConfiguration)
class DeleteExecutionCompensationTest extends QuizzesFull2SpockTest {

    Integer courseAggregateId
    Integer executionAggregateId
    Integer userAggregateId

    def setup() {
        loadBehaviorScripts()
        courseAggregateId = createCourse()
        executionAggregateId = createExecution(courseAggregateId)
        userAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, userAggregateId)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "deleteExecution: fault on deleteExecutionStep compensates the lock acquired by getExecutionStep"() {
        // Spec: plan.md §4 Execution — DeleteExecution(executionAggregateId); saga state IN_DELETE_EXECUTION
        when:
        executionFunctionalities.deleteExecution(executionAggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(executionAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the soft-delete never ran: the execution is still readable in its pre-saga state'
        def reread = executionFunctionalities.getExecutionById(executionAggregateId)
        reread.acronym == EXECUTION_ACRONYM
        reread.academicTerm == EXECUTION_ACADEMIC_TERM
        reread.courseAggregateId == courseAggregateId

        and: 'the roster was not cleared either'
        reread.students.collect { it.userAggregateId } == [userAggregateId]
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
