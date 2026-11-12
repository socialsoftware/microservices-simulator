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
class GetUserByIdTest extends TrainticketSpockTest {

    def "getUserById: success"() {
        // Spec: plan.md §3 User - GetUserById
        given: 'a user exists'
        def userAggregateId = createUser(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)

        when:
        def result = userFunctionalities.getUserById(userAggregateId)

        then: 'the saga returns a coherent user DTO'
        result.aggregateId == userAggregateId
        result.userName == USER_NAME
        result.password == USER_PASSWORD
        result.gender == USER_GENDER
        result.documentType == USER_DOCUMENT_TYPE
        result.documentNumber == USER_DOCUMENT_NUMBER
        result.email == USER_EMAIL
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
