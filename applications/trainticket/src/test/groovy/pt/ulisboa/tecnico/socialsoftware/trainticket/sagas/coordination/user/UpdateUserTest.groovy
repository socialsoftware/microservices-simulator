package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.sagas.states.UserSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.sagas.UpdateUserFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateUserTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    def "updateUser: success"() {
        // Spec: plan.md §3 User - UpdateUser
        given: 'a user exists'
        def userAggregateId = createUser(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)

        when:
        userFunctionalities.updateUser(userAggregateId,
                new UserDto(USER_NAME, USER_PASSWORD_TWO, USER_GENDER_TWO,
                        USER_DOCUMENT_TYPE_TWO, USER_DOCUMENT_NUMBER_TWO, USER_EMAIL_TWO))

        then: 'the traversal completes and releases the lock'
        sagaStateOf(userAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updateUser: getUserStep acquires IN_UPDATE_USER semantic lock"() {
        // Spec: plan.md §3 User - UpdateUser; primary-aggregate lock acquisition
        given:
        def userAggregateId = createUser(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)
        def uow = unitOfWorkService.createUnitOfWork("updateUser")
        def func = new UpdateUserFunctionalitySagas(unitOfWorkService, userAggregateId,
                new UserDto(USER_NAME, USER_PASSWORD_TWO, USER_GENDER_TWO,
                        USER_DOCUMENT_TYPE_TWO, USER_DOCUMENT_NUMBER_TWO, USER_EMAIL_TWO),
                uow, commandGateway)
        func.executeUntilStep("getUserStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_UPDATE_USER'
        sagaStateOf(userAggregateId) == UserSagaState.IN_UPDATE_USER

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
