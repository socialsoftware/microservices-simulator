package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

@DataJpaTest
@Transactional
@Import(DeleteUserCompensationTest.LocalBeanConfiguration)
class DeleteUserCompensationTest extends QuizzesFull2SpockTest {

    Integer userAggregateId

    def setup() {
        loadBehaviorScripts()
        userAggregateId = createUser()
        userFunctionalities.activateUser(userAggregateId)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "deleteUser: fault on deleteUserStep compensates the lock acquired by getUserStep"() {
        // Spec: plan.md §2 User — DeleteUser(userAggregateId); saga state IN_DELETE_USER
        when:
        userFunctionalities.deleteUser(userAggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(userAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the soft-delete never ran: the user is still readable in its pre-saga state'
        def reread = userFunctionalities.getUserById(userAggregateId)
        reread.active
        reread.name == USER_NAME
        reread.username == USER_USERNAME
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
