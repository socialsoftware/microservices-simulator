package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateUserTest extends TrainticketSpockTest {

    def "createUser: success"() {
        // Spec: plan.md §3 User - CreateUser
        given: 'a user request'
        def userDto = new UserDto(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)

        when:
        def result = userFunctionalities.createUser(userDto)

        then: 'the saga returns a coherent user DTO'
        result.aggregateId != null
        result.userName == USER_NAME
        result.password == USER_PASSWORD
        result.gender == USER_GENDER
        result.documentType == USER_DOCUMENT_TYPE
        result.documentNumber == USER_DOCUMENT_NUMBER
        result.email == USER_EMAIL

        and: 'the saga left no lock behind'
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
