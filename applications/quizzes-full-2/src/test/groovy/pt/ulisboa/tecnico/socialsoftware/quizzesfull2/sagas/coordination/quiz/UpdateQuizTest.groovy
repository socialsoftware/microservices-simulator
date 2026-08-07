package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.quiz

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.sagas.states.QuizSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.sagas.UpdateQuizFunctionalitySagas

@DataJpaTest
@Transactional
@Import(UpdateQuizTest.LocalBeanConfiguration)
class UpdateQuizTest extends QuizzesFull2SpockTest {

    public static final String UPDATED_QUIZ_TITLE = "Sorting and searching quiz"
    public static final Integer NONEXISTENT_QUESTION_AGGREGATE_ID = 999998

    @Autowired
    LocalCommandGateway commandGateway

    def "updateQuiz: success"() {
        // Spec: plan.md §6 Quiz — UpdateQuiz(quizAggregateId, title, availableDate, conclusionDate,
        // resultsDate, questionAggregateIds)
        given: 'a quiz and a question of its course'
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def questionAggregateId = createQuestion(courseAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId)

        when:
        quizFunctionalities.updateQuiz(quizAggregateId, UPDATED_QUIZ_TITLE, QUIZ_AVAILABLE_DATE,
                QUIZ_CONCLUSION_DATE, QUIZ_RESULTS_DATE, [questionAggregateId])

        then: 'orchestration outcome only — persistence is asserted in T2'
        sagaStateOf(quizAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updateQuiz: aborts when a question prerequisite does not exist"() {
        // Spec: plan.md §6 Quiz — cross-aggregate prerequisite (P4a): getQuestionsStep fetches each
        // question and throws if one does not exist, so no explicit guard is written
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId)

        when:
        quizFunctionalities.updateQuiz(quizAggregateId, UPDATED_QUIZ_TITLE, QUIZ_AVAILABLE_DATE,
                QUIZ_CONCLUSION_DATE, QUIZ_RESULTS_DATE, [NONEXISTENT_QUESTION_AGGREGATE_ID])

        then:
        thrown(SimulatorException)
    }

    def "updateQuiz: getQuizStep acquires IN_UPDATE_QUIZ semantic lock"() {
        // Spec: plan.md §6 Quiz — saga state IN_UPDATE_QUIZ acquired by the primary lock step
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId)
        def uow = unitOfWorkService.createUnitOfWork("updateQuiz")
        def func = new UpdateQuizFunctionalitySagas(unitOfWorkService, quizAggregateId, UPDATED_QUIZ_TITLE,
                QUIZ_AVAILABLE_DATE, QUIZ_CONCLUSION_DATE, QUIZ_RESULTS_DATE, [], uow, commandGateway)
        func.executeUntilStep("getQuizStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_UPDATE_QUIZ'
        sagaStateOf(quizAggregateId) == QuizSagaState.IN_UPDATE_QUIZ

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
