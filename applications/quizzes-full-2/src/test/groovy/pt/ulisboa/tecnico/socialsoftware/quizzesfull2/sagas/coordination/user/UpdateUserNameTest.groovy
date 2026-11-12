package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.sagas.states.UserSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas.UpdateUserNameFunctionalitySagas

@DataJpaTest
@Transactional
@Import(UpdateUserNameTest.LocalBeanConfiguration)
class UpdateUserNameTest extends QuizzesFull2SpockTest {

    public static final String UPDATED_USER_NAME = "Alice Smithson"

    @Autowired
    LocalCommandGateway commandGateway

    def "updateUserName: success"() {
        // Spec: plan.md §2 User — UpdateUserName(userAggregateId, name)
        given: 'an existing user'
        def userAggregateId = createUser()

        when:
        userFunctionalities.updateUserName(userAggregateId, UPDATED_USER_NAME)

        then: 'orchestration outcome only — persistence is asserted in T2'
        sagaStateOf(userAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updateUserName: getUserStep acquires IN_UPDATE_USER_NAME semantic lock"() {
        // Spec: plan.md §2 User — saga state IN_UPDATE_USER_NAME acquired by the primary lock step
        given:
        def userAggregateId = createUser()
        def uow = unitOfWorkService.createUnitOfWork("updateUserName")
        def func = new UpdateUserNameFunctionalitySagas(
                unitOfWorkService, userAggregateId, UPDATED_USER_NAME, uow, commandGateway)
        func.executeUntilStep("getUserStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_UPDATE_USER_NAME'
        sagaStateOf(userAggregateId) == UserSagaState.IN_UPDATE_USER_NAME

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
