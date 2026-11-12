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
@Import(UpdateUserNameCompensationTest.LocalBeanConfiguration)
class UpdateUserNameCompensationTest extends QuizzesFull2SpockTest {

    Integer userAggregateId

    def setup() {
        loadBehaviorScripts()
        userAggregateId = createUser()
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updateUserName: fault on updateUserNameStep compensates the lock acquired by getUserStep"() {
        // Spec: plan.md §2 User — UpdateUserName(userAggregateId, name); saga state IN_UPDATE_USER_NAME
        when:
        userFunctionalities.updateUserName(userAggregateId, "Alice Smithson")

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(userAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: read-back shows the pre-saga state'
        def reread = userFunctionalities.getUserById(userAggregateId)
        reread.name == USER_NAME
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
