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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas.UpdateExecutionFunctionalitySagas

@DataJpaTest
@Transactional
@Import(UpdateExecutionTest.LocalBeanConfiguration)
class UpdateExecutionTest extends QuizzesFull2SpockTest {

    public static final String UPDATED_EXECUTION_ACRONYM = "SE-02"
    public static final String UPDATED_EXECUTION_ACADEMIC_TERM = "2026/2027"

    @Autowired
    LocalCommandGateway commandGateway

    def "updateExecution: success"() {
        // Spec: plan.md §4 Execution — UpdateExecution(executionAggregateId, acronym, academicTerm)
        given: 'an existing execution'
        def executionAggregateId = createExecution(createCourse())

        when:
        executionFunctionalities.updateExecution(executionAggregateId, UPDATED_EXECUTION_ACRONYM,
                UPDATED_EXECUTION_ACADEMIC_TERM)

        then: 'orchestration outcome only — persistence is asserted in T2'
        sagaStateOf(executionAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updateExecution: getExecutionStep acquires IN_UPDATE_EXECUTION semantic lock"() {
        // Spec: plan.md §4 Execution — saga state IN_UPDATE_EXECUTION acquired by the primary lock step
        given:
        def executionAggregateId = createExecution(createCourse())
        def uow = unitOfWorkService.createUnitOfWork("updateExecution")
        def func = new UpdateExecutionFunctionalitySagas(unitOfWorkService, executionAggregateId,
                UPDATED_EXECUTION_ACRONYM, UPDATED_EXECUTION_ACADEMIC_TERM, uow, commandGateway)
        func.executeUntilStep("getExecutionStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_UPDATE_EXECUTION'
        sagaStateOf(executionAggregateId) == ExecutionSagaState.IN_UPDATE_EXECUTION

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
