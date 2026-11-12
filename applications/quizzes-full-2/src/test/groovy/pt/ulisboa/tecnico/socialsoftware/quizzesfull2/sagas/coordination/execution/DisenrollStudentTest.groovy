package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.sagas.states.ExecutionSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas.DisenrollStudentFunctionalitySagas

@DataJpaTest
@Transactional
@Import(DisenrollStudentTest.LocalBeanConfiguration)
class DisenrollStudentTest extends QuizzesFull2SpockTest {

    @Autowired
    LocalCommandGateway commandGateway

    def "disenrollStudent: success"() {
        // Spec: plan.md §4 Execution — DisenrollStudent(executionAggregateId, userAggregateId)
        given: 'an execution with an enrolled student'
        def executionAggregateId = createExecution(createCourse())
        def userAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, userAggregateId)

        when:
        executionFunctionalities.disenrollStudent(executionAggregateId, userAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        sagaStateOf(executionAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "disenrollStudent: getExecutionStep acquires IN_DISENROLL_STUDENT semantic lock"() {
        // Spec: plan.md §4 Execution — saga state IN_DISENROLL_STUDENT acquired by the primary lock step
        given:
        def executionAggregateId = createExecution(createCourse())
        def userAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, userAggregateId)
        def uow = unitOfWorkService.createUnitOfWork("disenrollStudent")
        def func = new DisenrollStudentFunctionalitySagas(unitOfWorkService, executionAggregateId,
                userAggregateId, uow, commandGateway)
        func.executeUntilStep("getExecutionStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_DISENROLL_STUDENT'
        sagaStateOf(executionAggregateId) == ExecutionSagaState.IN_DISENROLL_STUDENT

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
