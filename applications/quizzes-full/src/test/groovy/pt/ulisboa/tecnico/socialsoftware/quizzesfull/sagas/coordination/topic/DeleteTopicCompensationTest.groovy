package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.topic

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteTopicCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String TOPIC_NAME_1 = "Algorithms"

    def courseDto
    def topicDto

    def setup() {
        loadBehaviorScripts()
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        topicDto = createTopic(courseDto.aggregateId, TOPIC_NAME_1)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "deleteTopic: fault on deleteTopicStep compensates the lock acquired by getTopicStep"() {
        // getTopicStep has already set TopicSagaState.READ_TOPIC and registered its compensation
        // by the time deleteTopicStep's injected fault fires, so this exercises the saga's own
        // compensate transition (READ_TOPIC -> NOT_IN_SAGA) with no second saga involved.
        when:
        topicFunctionalities.deleteTopic(topicDto.aggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(topicDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: topic is still readable, not deleted'
        def reread = topicFunctionalities.getTopicById(topicDto.aggregateId)
        reread.aggregateId == topicDto.aggregateId
    }
}
