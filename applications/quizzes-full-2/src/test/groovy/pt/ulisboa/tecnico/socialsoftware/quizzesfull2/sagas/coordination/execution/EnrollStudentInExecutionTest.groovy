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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.sagas.states.ExecutionSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas.EnrollStudentInExecutionFunctionalitySagas

@DataJpaTest
@Transactional
@Import(EnrollStudentInExecutionTest.LocalBeanConfiguration)
class EnrollStudentInExecutionTest extends QuizzesFull2SpockTest {

    @Autowired
    LocalCommandGateway commandGateway

    def "enrollStudentInExecution: success"() {
        // Spec: plan.md §4 Execution — EnrollStudentInExecution(executionAggregateId, userAggregateId)
        given: 'an execution and an active user'
        def executionAggregateId = createExecution(createCourse())
        def userAggregateId = createActiveUser()

        when:
        executionFunctionalities.enrollStudentInExecution(executionAggregateId, userAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        sagaStateOf(executionAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "enrollStudentInExecution: INACTIVE_USER violation"() {
        // Spec: plan.md §3.2 — rule INACTIVE_USER (P3, validated on the UserDto the saga assembles)
        given: 'a user that was never activated'
        def executionAggregateId = createExecution(createCourse())
        def userAggregateId = createUser()

        when:
        executionFunctionalities.enrollStudentInExecution(executionAggregateId, userAggregateId)

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.INACTIVE_USER
    }

    def "enrollStudentInExecution: getExecutionStep acquires IN_ENROLL_STUDENT_IN_EXECUTION semantic lock"() {
        // Spec: plan.md §4 Execution — saga state IN_ENROLL_STUDENT_IN_EXECUTION acquired by the primary lock step
        given:
        def executionAggregateId = createExecution(createCourse())
        def userAggregateId = createActiveUser()
        def uow = unitOfWorkService.createUnitOfWork("enrollStudentInExecution")
        def func = new EnrollStudentInExecutionFunctionalitySagas(unitOfWorkService, executionAggregateId,
                userAggregateId, uow, commandGateway)
        func.executeUntilStep("getExecutionStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_ENROLL_STUDENT_IN_EXECUTION'
        sagaStateOf(executionAggregateId) == ExecutionSagaState.IN_ENROLL_STUDENT_IN_EXECUTION

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
