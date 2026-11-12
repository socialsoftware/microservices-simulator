package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.question

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.sagas.states.QuestionSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas.DeleteQuestionFunctionalitySagas

@DataJpaTest
@Transactional
@Import(DeleteQuestionTest.LocalBeanConfiguration)
class DeleteQuestionTest extends QuizzesFull2SpockTest {

    @Autowired
    LocalCommandGateway commandGateway

    // No happy-path case: deleteQuestion makes its own aggregate unresolvable, so sagaStateOf throws.
    // See docs/concepts/testing.md § T4 — "Exception — a functionality whose success makes its own
    // aggregate unresolvable". The delete's effect is asserted in QuestionServiceTest (T2).

    def "deleteQuestion: getQuestionStep acquires IN_DELETE_QUESTION semantic lock"() {
        // Spec: plan.md §5 Question — saga state IN_DELETE_QUESTION acquired by the primary lock step
        given:
        def courseAggregateId = createCourse()
        def questionAggregateId = createQuestion(courseAggregateId)
        def uow = unitOfWorkService.createUnitOfWork("deleteQuestion")
        def func = new DeleteQuestionFunctionalitySagas(
                unitOfWorkService, questionAggregateId, uow, commandGateway)
        func.executeUntilStep("getQuestionStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_DELETE_QUESTION'
        sagaStateOf(questionAggregateId) == QuestionSagaState.IN_DELETE_QUESTION

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
