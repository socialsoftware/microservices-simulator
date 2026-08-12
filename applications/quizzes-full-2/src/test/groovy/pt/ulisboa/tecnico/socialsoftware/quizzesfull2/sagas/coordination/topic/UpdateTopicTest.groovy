package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.topic

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.sagas.states.TopicSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.sagas.UpdateTopicFunctionalitySagas

@DataJpaTest
@Transactional
@Import(UpdateTopicTest.LocalBeanConfiguration)
class UpdateTopicTest extends QuizzesFull2SpockTest {

    public static final String UPDATED_TOPIC_NAME = "Advanced Algorithms"

    @Autowired
    LocalCommandGateway commandGateway

    def "updateTopic: success"() {
        // Spec: plan.md §3 Topic — UpdateTopic(topicAggregateId, name)
        given: 'an existing topic'
        def topicAggregateId = createTopic(createCourse())

        when:
        topicFunctionalities.updateTopic(topicAggregateId, UPDATED_TOPIC_NAME)

        then: 'orchestration outcome only — persistence is asserted in T2'
        sagaStateOf(topicAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updateTopic: getTopicStep acquires IN_UPDATE_TOPIC semantic lock"() {
        // Spec: plan.md §3 Topic — saga state IN_UPDATE_TOPIC acquired by the primary lock step
        given:
        def topicAggregateId = createTopic(createCourse())
        def uow = unitOfWorkService.createUnitOfWork("updateTopic")
        def func = new UpdateTopicFunctionalitySagas(
                unitOfWorkService, topicAggregateId, UPDATED_TOPIC_NAME, uow, commandGateway)
        func.executeUntilStep("getTopicStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_UPDATE_TOPIC'
        sagaStateOf(topicAggregateId) == TopicSagaState.IN_UPDATE_TOPIC

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
