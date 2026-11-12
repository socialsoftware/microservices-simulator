package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.sagas.states.UserSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas.DeleteUserFunctionalitySagas

@DataJpaTest
@Transactional
@Import(DeleteUserTest.LocalBeanConfiguration)
class DeleteUserTest extends QuizzesFull2SpockTest {

    @Autowired
    LocalCommandGateway commandGateway

    // No happy-path case: deleteUser makes its own aggregate unresolvable, so sagaStateOf throws.
    // See docs/concepts/testing.md § T4 — "Exception — a functionality whose success makes its own
    // aggregate unresolvable". The delete's effect is asserted in UserServiceTest (T2).

    def "deleteUser: getUserStep acquires IN_DELETE_USER semantic lock"() {
        // Spec: plan.md §2 User — saga state IN_DELETE_USER acquired by the primary lock step
        given:
        def userAggregateId = createUser()
        def uow = unitOfWorkService.createUnitOfWork("deleteUser")
        def func = new DeleteUserFunctionalitySagas(
                unitOfWorkService, userAggregateId, uow, commandGateway)
        func.executeUntilStep("getUserStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_DELETE_USER'
        sagaStateOf(userAggregateId) == UserSagaState.IN_DELETE_USER

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
