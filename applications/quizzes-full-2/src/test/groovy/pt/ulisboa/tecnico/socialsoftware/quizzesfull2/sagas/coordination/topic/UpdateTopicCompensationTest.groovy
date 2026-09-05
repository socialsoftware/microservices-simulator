package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.topic

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

@DataJpaTest
@Transactional
@Import(UpdateTopicCompensationTest.LocalBeanConfiguration)
class UpdateTopicCompensationTest extends QuizzesFull2SpockTest {

    Integer courseAggregateId
    Integer topicAggregateId

    def setup() {
        loadBehaviorScripts()
        courseAggregateId = createCourse()
        topicAggregateId = createTopic(courseAggregateId)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updateTopic: fault on updateTopicStep compensates the lock acquired by getTopicStep"() {
        // Spec: plan.md §3 Topic — UpdateTopic(topicAggregateId, name); saga state IN_UPDATE_TOPIC
        when:
        topicFunctionalities.updateTopic(topicAggregateId, "Advanced Algorithms")

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(topicAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the update never ran: the topic still carries its pre-saga name'
        def reread = topicFunctionalities.getTopicsByCourse(courseAggregateId)
                .find { it.aggregateId == topicAggregateId }
        reread.name == TOPIC_NAME
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
