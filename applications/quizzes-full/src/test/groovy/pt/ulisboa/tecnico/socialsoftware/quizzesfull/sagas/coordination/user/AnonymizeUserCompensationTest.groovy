package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class AnonymizeUserCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def userDto

    def setup() {
        loadBehaviorScripts()
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "anonymizeUser: fault on anonymizeUserStep compensates the lock acquired by getUserStep"() {
        // getUserStep has already set UserSagaState.READ_USER and registered its compensation by
        // the time anonymizeUserStep's injected fault fires, so this exercises the saga's own
        // compensate transition (READ_USER -> NOT_IN_SAGA) with no second saga involved.
        when:
        userFunctionalities.anonymizeUser(userDto.aggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(userDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: user is not anonymized on read-back'
        def reread = userFunctionalities.getUserById(userDto.aggregateId)
        reread.name == USER_NAME_1
        reread.username == USER_USERNAME_1
    }
}
