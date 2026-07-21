package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.states.ExecutionSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas.DeleteExecutionFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteExecutionTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    def courseDto
    def executionDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
    }

    def "deleteExecution: success"() {
        // Spec: plan.md §4 Execution — DeleteExecution; orchestration outcome only, persistence in ExecutionServiceTest.
        when:
        executionFunctionalities.deleteExecution(executionDto.aggregateId)

        then:
        noExceptionThrown()
    }

    def "deleteExecution: getExecutionStep acquires IN_DELETE_EXECUTION semantic lock"() {
        given: 'workflow pauses after getExecutionStep'
        def uow1 = unitOfWorkService.createUnitOfWork("deleteExecution")
        def func1 = new DeleteExecutionFunctionalitySagas(
                unitOfWorkService, executionDto.aggregateId, uow1, commandGateway)
        func1.executeUntilStep("getExecutionStep", uow1)

        expect: 'execution is locked'
        sagaStateOf(executionDto.aggregateId) == ExecutionSagaState.IN_DELETE_EXECUTION

        when: 'workflow resumes and completes'
        func1.resumeWorkflow(uow1)

        then:
        noExceptionThrown()
    }
}
