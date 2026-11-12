package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.sagas.SagaUser

@DataJpaTest
@Transactional
@Import(UserIntraInvariantTest.LocalBeanConfiguration)
class UserIntraInvariantTest extends QuizzesFull2SpockTest {

    def "create user"() {
        // Spec: plan.md §2 User — CreateUser(name, username, role); fields name, username, role, active
        when:
        def user = new SagaUser(USER_AGGREGATE_ID, USER_NAME, USER_USERNAME, USER_ROLE)
        user.verifyInvariants()

        then:
        user.aggregateId == USER_AGGREGATE_ID
        user.name == USER_NAME
        user.username == USER_USERNAME
        user.role == USER_ROLE
        !user.isActive()
        user.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    def "user: USER_DELETED_STATE violation"() {
        // Spec: plan.md §2 User — rule USER_DELETED_STATE (state == DELETED => active == false)
        given:
        def user = new SagaUser(USER_AGGREGATE_ID, USER_NAME, USER_USERNAME, USER_ROLE)
        user.setActive(true)
        user.remove()

        when:
        user.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.USER_DELETED_STATE
    }

    def "user: USER_DELETED_STATE holds when a deleted user is inactive"() {
        // Spec: plan.md §2 User — rule USER_DELETED_STATE, satisfying representative
        given:
        def user = new SagaUser(USER_AGGREGATE_ID, USER_NAME, USER_USERNAME, USER_ROLE)
        user.setActive(true)
        user.setActive(false)
        user.remove()

        when:
        user.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    // USER_ROLE_FINAL is a Java `final` field — the compiler enforces it, so testing.md § T1
    // requires no violation case. USER_DELETED_STATE is a categorical state freeze, so no
    // boundary-straddling pair applies.

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
