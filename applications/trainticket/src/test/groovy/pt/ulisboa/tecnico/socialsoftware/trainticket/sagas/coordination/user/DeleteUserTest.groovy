package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.sagas.states.UserSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.sagas.DeleteUserFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteUserTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    // No happy-path case: deleteUser makes its own aggregate unresolvable, so sagaStateOf throws.
    // See docs/concepts/testing.md § T4 - "Exception — a functionality whose success makes
    // its own aggregate unresolvable". The delete's effect is asserted in UserServiceTest (T2).

    def "deleteUser: getUserStep acquires IN_DELETE_USER semantic lock"() {
        // Spec: plan.md §3 User - DeleteUser; primary-aggregate lock acquisition
        given:
        def userAggregateId = createUser(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)
        def uow = unitOfWorkService.createUnitOfWork("deleteUser")
        def func = new DeleteUserFunctionalitySagas(unitOfWorkService, userAggregateId,
                uow, commandGateway)
        func.executeUntilStep("getUserStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_DELETE_USER'
        sagaStateOf(userAggregateId) == UserSagaState.IN_DELETE_USER

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
