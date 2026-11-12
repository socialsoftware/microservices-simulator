package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.question

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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.sagas.states.QuestionSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas.UpdateQuestionFunctionalitySagas

@DataJpaTest
@Transactional
@Import(UpdateQuestionTest.LocalBeanConfiguration)
class UpdateQuestionTest extends QuizzesFull2SpockTest {

    public static final String UPDATED_QUESTION_TITLE = "Merge sort complexity"
    public static final String UPDATED_QUESTION_CONTENT = "State the worst-case complexity of merge sort."
    public static final Integer NONEXISTENT_TOPIC_AGGREGATE_ID = 999998

    @Autowired
    LocalCommandGateway commandGateway

    def "updateQuestion: success"() {
        // Spec: plan.md §5 Question — UpdateQuestion(questionAggregateId, title, content, topicAggregateIds)
        given: 'a question and a topic of its course'
        def courseAggregateId = createCourse()
        def topicAggregateId = createTopic(courseAggregateId)
        def questionAggregateId = createQuestion(courseAggregateId)

        when:
        questionFunctionalities.updateQuestion(questionAggregateId, UPDATED_QUESTION_TITLE,
                UPDATED_QUESTION_CONTENT, [topicAggregateId])

        then: 'orchestration outcome only — persistence is asserted in T2'
        sagaStateOf(questionAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updateQuestion: aborts when a topic prerequisite does not exist"() {
        // Spec: plan.md §5 Question — cross-aggregate prerequisite (P4a): getTopicsStep fetches each
        // topic and throws if one does not exist, so no explicit guard is written
        given:
        def courseAggregateId = createCourse()
        def questionAggregateId = createQuestion(courseAggregateId)

        when:
        questionFunctionalities.updateQuestion(questionAggregateId, UPDATED_QUESTION_TITLE,
                UPDATED_QUESTION_CONTENT, [NONEXISTENT_TOPIC_AGGREGATE_ID])

        then:
        thrown(SimulatorException)
    }

    def "updateQuestion: getQuestionStep acquires IN_UPDATE_QUESTION semantic lock"() {
        // Spec: plan.md §5 Question — saga state IN_UPDATE_QUESTION acquired by the primary lock step
        given:
        def courseAggregateId = createCourse()
        def questionAggregateId = createQuestion(courseAggregateId)
        def uow = unitOfWorkService.createUnitOfWork("updateQuestion")
        def func = new UpdateQuestionFunctionalitySagas(unitOfWorkService, questionAggregateId,
                UPDATED_QUESTION_TITLE, UPDATED_QUESTION_CONTENT, [], uow, commandGateway)
        func.executeUntilStep("getQuestionStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_UPDATE_QUESTION'
        sagaStateOf(questionAggregateId) == QuestionSagaState.IN_UPDATE_QUESTION

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
