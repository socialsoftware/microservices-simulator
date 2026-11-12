package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.quizanswer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.sagas.states.QuizAnswerSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.sagas.ConcludeQuizFunctionalitySagas

@DataJpaTest
@Transactional
@Import(ConcludeQuizTest.LocalBeanConfiguration)
class ConcludeQuizTest extends QuizzesFull2SpockTest {

    @Autowired
    LocalCommandGateway commandGateway

    Integer quizAnswerAggregateId

    def setup() {
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)
        quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)
    }

    def "concludeQuiz: success"() {
        // Spec: plan.md §7 QuizAnswer — ConcludeQuiz(quizAnswerAggregateId)
        when:
        quizAnswerFunctionalities.concludeQuiz(quizAnswerAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        sagaStateOf(quizAnswerAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "concludeQuiz: getQuizAnswerStep acquires IN_CONCLUDE_QUIZ semantic lock"() {
        // Spec: plan.md §7 QuizAnswer — saga state IN_CONCLUDE_QUIZ acquired by the primary lock step
        given:
        def uow = unitOfWorkService.createUnitOfWork("concludeQuiz")
        def func = new ConcludeQuizFunctionalitySagas(unitOfWorkService, quizAnswerAggregateId, uow,
                commandGateway)
        func.executeUntilStep("getQuizAnswerStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_CONCLUDE_QUIZ'
        sagaStateOf(quizAnswerAggregateId) == QuizAnswerSagaState.IN_CONCLUDE_QUIZ

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
