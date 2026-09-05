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
@Import(EnrollStudentInExecutionCompensationTest.LocalBeanConfiguration)
class EnrollStudentInExecutionCompensationTest extends QuizzesFull2SpockTest {

    Integer executionAggregateId
    Integer userAggregateId

    def setup() {
        loadBehaviorScripts()
        executionAggregateId = createExecution(createCourse())
        userAggregateId = createActiveUser()
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "enrollStudentInExecution: fault on enrollStudentStep compensates the lock acquired by getExecutionStep"() {
        // Spec: plan.md §4 Execution — EnrollStudentInExecution; saga state IN_ENROLL_STUDENT_IN_EXECUTION
        when:
        executionFunctionalities.enrollStudentInExecution(executionAggregateId, userAggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(executionAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the enrolment never ran: the execution roster is still empty'
        def reread = executionFunctionalities.getExecutionById(executionAggregateId)
        reread.students.isEmpty()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
