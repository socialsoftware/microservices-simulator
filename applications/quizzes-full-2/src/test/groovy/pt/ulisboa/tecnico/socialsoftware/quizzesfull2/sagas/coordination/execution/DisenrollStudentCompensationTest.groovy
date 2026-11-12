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
@Import(DisenrollStudentCompensationTest.LocalBeanConfiguration)
class DisenrollStudentCompensationTest extends QuizzesFull2SpockTest {

    Integer executionAggregateId
    Integer userAggregateId

    def setup() {
        loadBehaviorScripts()
        executionAggregateId = createExecution(createCourse())
        userAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, userAggregateId)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "disenrollStudent: fault on disenrollStudentStep compensates the lock acquired by getExecutionStep"() {
        // Spec: plan.md §4 Execution — DisenrollStudent; saga state IN_DISENROLL_STUDENT
        when:
        executionFunctionalities.disenrollStudent(executionAggregateId, userAggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(executionAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the disenrolment never ran: the student is still on the roster'
        def reread = executionFunctionalities.getExecutionById(executionAggregateId)
        reread.students.collect { it.userAggregateId } == [userAggregateId]
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
