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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.coordination.sagas.UpdateUserNameFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateUserNameTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    UserDto userDto

    def setup() {
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
    }

    def "updateUserName: success"() {
        // Spec: plan.md §2 User — UpdateUserName; orchestration outcome only, persistence in UserServiceTest.
        when:
        userFunctionalities.updateUserName(userDto.aggregateId, USER_NAME_2)

        then:
        noExceptionThrown()
        sagaStateOf(userDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updateUserName: getUserStep acquires READ_USER semantic lock"() {
        given: 'an updateUserName workflow pauses after getUserStep has acquired READ_USER lock'
        def uow = unitOfWorkService.createUnitOfWork("updateUserName")
        def func = new UpdateUserNameFunctionalitySagas(
                unitOfWorkService, userDto.aggregateId, USER_NAME_2, uow, commandGateway)
        func.executeUntilStep("getUserStep", uow)

        expect: 'user saga state is READ_USER'
        sagaStateOf(userDto.aggregateId) == UserSagaState.READ_USER

        when: 'workflow resumes and completes'
        func.resumeWorkflow(uow)

        then:
        noExceptionThrown()
    }
}
