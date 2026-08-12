package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.sagas.states.ExecutionSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas.DeleteExecutionFunctionalitySagas

@DataJpaTest
@Transactional
@Import(DeleteExecutionTest.LocalBeanConfiguration)
class DeleteExecutionTest extends QuizzesFull2SpockTest {

    @Autowired
    LocalCommandGateway commandGateway

    // No happy-path case: deleteExecution makes its own aggregate unresolvable, so sagaStateOf throws.
    // See docs/concepts/testing.md § T4 — "Exception — a functionality whose success makes its own
    // aggregate unresolvable". The delete's effect is asserted in ExecutionServiceTest (T2).

    def "deleteExecution: getExecutionStep acquires IN_DELETE_EXECUTION semantic lock"() {
        // Spec: plan.md §4 Execution — saga state IN_DELETE_EXECUTION acquired by the primary lock step
        given:
        def executionAggregateId = createExecution(createCourse())
        def uow = unitOfWorkService.createUnitOfWork("deleteExecution")
        def func = new DeleteExecutionFunctionalitySagas(
                unitOfWorkService, executionAggregateId, uow, commandGateway)
        func.executeUntilStep("getExecutionStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_DELETE_EXECUTION'
        sagaStateOf(executionAggregateId) == ExecutionSagaState.IN_DELETE_EXECUTION

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
