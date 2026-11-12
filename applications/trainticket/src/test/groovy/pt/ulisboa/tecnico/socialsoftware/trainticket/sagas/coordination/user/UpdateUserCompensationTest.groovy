package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateUserCompensationTest extends TrainticketSpockTest {

    def userAggregateId

    def setup() {
        loadBehaviorScripts()
        userAggregateId = createUser(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updateUser: fault on updateUserStep compensates the lock acquired by getUserStep"() {
        // Spec: plan.md §3 User - UpdateUser; compensate transition
        when:
        userFunctionalities.updateUser(userAggregateId,
                new UserDto(USER_NAME, USER_PASSWORD_TWO, USER_GENDER_TWO,
                        USER_DOCUMENT_TYPE_TWO, USER_DOCUMENT_NUMBER_TWO, USER_EMAIL_TWO))

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(userAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: read-back shows the pre-saga state'
        def reread = userFunctionalities.getUserById(userAggregateId)
        reread.password == USER_PASSWORD
        reread.gender == USER_GENDER
        reread.documentType == USER_DOCUMENT_TYPE
        reread.documentNumber == USER_DOCUMENT_NUMBER
        reread.email == USER_EMAIL
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
