package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto

@DataJpaTest
@Transactional
@Import(CreateUserTest.LocalBeanConfiguration)
class CreateUserTest extends QuizzesFull2SpockTest {

    def "createUser: success"() {
        // Spec: plan.md §2 User — CreateUser(name, username, role)
        given: 'a user to create'
        def userDto = new UserDto()
        userDto.setName(USER_NAME)
        userDto.setUsername(USER_USERNAME)
        userDto.setRole(USER_ROLE)

        when:
        def result = userFunctionalities.createUser(userDto)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.aggregateId != null
        result.name == USER_NAME
        result.username == USER_USERNAME
        result.role == USER_ROLE
        !result.active
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    // Semantic-lock acquisition: CreateUserFunctionalitySagas has no setSemanticLock step —
    // User is a DAG root and the create step brings the aggregate into existence
    // (sagas.md § Create Functionality Sagas), so there is no prior state to lock.

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
