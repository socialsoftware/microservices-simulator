package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas.states.UserSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.coordination.sagas.DeleteUserFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteUserTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    UserDto userDto

    def setup() {
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
    }

    def "deleteUser: success"() {
        when:
        userFunctionalities.deleteUser(userDto.aggregateId)

        and: 'verify user is no longer retrievable'
        def uow = unitOfWorkService.createUnitOfWork("verify")
        unitOfWorkService.aggregateLoadAndRegisterRead(userDto.aggregateId, uow)

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "deleteUser: deleteUserStep fails when user is deleted by concurrent deleteUser"() {
        given: 'deleteUser workflow pauses after getUserStep has acquired READ_USER lock'
        def uow1 = unitOfWorkService.createUnitOfWork("deleteUser")
        def func1 = new DeleteUserFunctionalitySagas(
                unitOfWorkService, userDto.aggregateId, uow1, commandGateway)
        func1.executeUntilStep("getUserStep", uow1)

        expect: 'user saga state is READ_USER'
        sagaStateOf(userDto.aggregateId) == UserSagaState.READ_USER

        and: 'concurrent deleteUser acquires READ_USER and completes deletion'
        def uow2 = unitOfWorkService.createUnitOfWork("deleteUser2")
        def func2 = new DeleteUserFunctionalitySagas(
                unitOfWorkService, userDto.aggregateId, uow2, commandGateway)
        func2.executeUntilStep("getUserStep", uow2)
        func2.resumeWorkflow(uow2)

        when: 'first deleteUser resumes into already-deleted user'
        func1.resumeWorkflow(uow1)

        then:
        thrown(SimulatorException)
    }
}
