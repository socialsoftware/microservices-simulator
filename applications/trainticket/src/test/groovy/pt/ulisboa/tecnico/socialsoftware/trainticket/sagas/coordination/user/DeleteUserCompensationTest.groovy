package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteUserCompensationTest extends TrainticketSpockTest {

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

    def "deleteUser: fault on deleteUserStep compensates the lock acquired by getUserStep"() {
        // Spec: plan.md §3 User - DeleteUser; compensate transition
        when:
        userFunctionalities.deleteUser(userAggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(userAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the soft delete never ran: read-back still resolves the user'
        def reread = userFunctionalities.getUserById(userAggregateId)
        reread.userName == USER_NAME
        reread.password == USER_PASSWORD
        reread.email == USER_EMAIL
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
