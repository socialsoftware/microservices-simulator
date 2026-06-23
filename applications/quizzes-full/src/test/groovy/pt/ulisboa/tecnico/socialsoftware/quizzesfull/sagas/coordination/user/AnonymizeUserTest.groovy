package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas.states.UserSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.coordination.sagas.AnonymizeUserFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class AnonymizeUserTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    UserDto userDto

    def setup() {
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
    }

    def "anonymizeUser: success — name and username set to ANONYMOUS"() {
        // Spec: User.{name,username} = "ANONYMOUS"; SagaState after commit == NOT_IN_SAGA.
        // Source: UserService.anonymizeUser.
        when:
        userFunctionalities.anonymizeUser(userDto.aggregateId)

        then:
        def result = userFunctionalities.getUserById(userDto.aggregateId)
        result.name == "ANONYMOUS"
        result.username == "ANONYMOUS"
        sagaStateOf(userDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "anonymizeUser: getUserStep acquires READ_USER semantic lock"() {
        given: 'an anonymizeUser workflow pauses after getUserStep has acquired READ_USER lock'
        def uow = unitOfWorkService.createUnitOfWork("anonymizeUser")
        def func = new AnonymizeUserFunctionalitySagas(
                unitOfWorkService, userDto.aggregateId, uow, commandGateway)
        func.executeUntilStep("getUserStep", uow)

        expect: 'user saga state is READ_USER'
        sagaStateOf(userDto.aggregateId) == UserSagaState.READ_USER

        when: 'workflow resumes and completes'
        func.resumeWorkflow(uow)

        then:
        noExceptionThrown()

        and: 'name and username were anonymized'
        def result = userFunctionalities.getUserById(userDto.aggregateId)
        result.name == "ANONYMOUS"
        result.username == "ANONYMOUS"
    }
}
