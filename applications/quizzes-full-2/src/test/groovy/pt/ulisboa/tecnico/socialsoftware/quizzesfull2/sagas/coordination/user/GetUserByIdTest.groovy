package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

@DataJpaTest
@Transactional
@Import(GetUserByIdTest.LocalBeanConfiguration)
class GetUserByIdTest extends QuizzesFull2SpockTest {

    def "getUserById: success"() {
        // Spec: plan.md §2 User — GetUserById(userAggregateId)
        given: 'a user exists'
        def userAggregateId = createUser()

        when:
        def result = userFunctionalities.getUserById(userAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.aggregateId == userAggregateId
        result.name == USER_NAME
        result.username == USER_USERNAME
        result.role == USER_ROLE
        sagaStateOf(userAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
