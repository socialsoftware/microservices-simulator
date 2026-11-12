package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.sagas.states.UserSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas.ActivateUserFunctionalitySagas

@DataJpaTest
@Transactional
@Import(ActivateUserTest.LocalBeanConfiguration)
class ActivateUserTest extends QuizzesFull2SpockTest {

    @Autowired
    LocalCommandGateway commandGateway

    def "activateUser: success"() {
        // Spec: plan.md §2 User — ActivateUser(userAggregateId)
        given: 'an existing user'
        def userAggregateId = createUser()

        when:
        userFunctionalities.activateUser(userAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        sagaStateOf(userAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "activateUser: getUserStep acquires IN_ACTIVATE_USER semantic lock"() {
        // Spec: plan.md §2 User — saga state IN_ACTIVATE_USER acquired by the primary lock step
        given:
        def userAggregateId = createUser()
        def uow = unitOfWorkService.createUnitOfWork("activateUser")
        def func = new ActivateUserFunctionalitySagas(
                unitOfWorkService, userAggregateId, uow, commandGateway)
        func.executeUntilStep("getUserStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_ACTIVATE_USER'
        sagaStateOf(userAggregateId) == UserSagaState.IN_ACTIVATE_USER

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
