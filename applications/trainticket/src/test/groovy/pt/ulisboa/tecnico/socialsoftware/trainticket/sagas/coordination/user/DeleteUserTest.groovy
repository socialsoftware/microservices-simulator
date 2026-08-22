package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
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

    def "deleteUser: success"() {
        // Spec: plan.md §3 User - DeleteUser (soft delete)
        given: 'a user exists'
        def userAggregateId = createUser(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)

        when:
        userFunctionalities.deleteUser(userAggregateId)

        and: 'attempt to load the now-deleted aggregate'
        unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then: 'DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

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
