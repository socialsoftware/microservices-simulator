package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.topic

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.sagas.states.TopicSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.sagas.DeleteTopicFunctionalitySagas

@DataJpaTest
@Transactional
@Import(DeleteTopicTest.LocalBeanConfiguration)
class DeleteTopicTest extends QuizzesFull2SpockTest {

    @Autowired
    LocalCommandGateway commandGateway

    // No happy-path case: deleteTopic makes its own aggregate unresolvable, so sagaStateOf throws.
    // See docs/concepts/testing.md § T4 — "Exception — a functionality whose success makes its own
    // aggregate unresolvable". The delete's effect is asserted in TopicServiceTest (T2).

    def "deleteTopic: getTopicStep acquires IN_DELETE_TOPIC semantic lock"() {
        // Spec: plan.md §3 Topic — saga state IN_DELETE_TOPIC acquired by the primary lock step
        given:
        def topicAggregateId = createTopic(createCourse())
        def uow = unitOfWorkService.createUnitOfWork("deleteTopic")
        def func = new DeleteTopicFunctionalitySagas(
                unitOfWorkService, topicAggregateId, uow, commandGateway)
        func.executeUntilStep("getTopicStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_DELETE_TOPIC'
        sagaStateOf(topicAggregateId) == TopicSagaState.IN_DELETE_TOPIC

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
