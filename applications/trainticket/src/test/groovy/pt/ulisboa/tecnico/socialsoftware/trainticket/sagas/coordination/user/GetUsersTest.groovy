package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetUsersTest extends TrainticketSpockTest {

    def "getUsers: success"() {
        // Spec: plan.md §3 User - GetUsers
        given: 'two users exist'
        def firstAggregateId = createUser(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)
        def secondAggregateId = createUser(USER_NAME_TWO, USER_PASSWORD_TWO, USER_GENDER_TWO,
                USER_DOCUMENT_TYPE_TWO, USER_DOCUMENT_NUMBER_TWO, USER_EMAIL_TWO)

        when:
        def result = userFunctionalities.getUsers()

        then: 'the saga returns a coherent DTO per user'
        result.size() == 2
        result.collect { it.aggregateId }.toSet() == [firstAggregateId, secondAggregateId].toSet()
        result.collect { it.userName }.toSet() == [USER_NAME, USER_NAME_TWO].toSet()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
