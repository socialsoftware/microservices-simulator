package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.topic

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateTopicCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String TOPIC_NAME_ORIGINAL = "Algorithms"
    public static final String TOPIC_NAME_UPDATED = "Advanced Algorithms"

    def courseDto
    def topicDto

    def setup() {
        loadBehaviorScripts()
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        topicDto = createTopic(courseDto.aggregateId, TOPIC_NAME_ORIGINAL)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updateTopic: fault on updateTopicStep compensates the lock acquired by getTopicStep"() {
        // getTopicStep has already set TopicSagaState.READ_TOPIC and registered its compensation
        // by the time updateTopicStep's injected fault fires, so this exercises the saga's own
        // compensate transition (READ_TOPIC -> NOT_IN_SAGA) with no second saga involved.
        given:
        TopicDto update = new TopicDto()
        update.aggregateId = topicDto.aggregateId
        update.name = TOPIC_NAME_UPDATED

        when:
        topicFunctionalities.updateTopic(update)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(topicDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: name is unchanged on read-back'
        def reread = topicFunctionalities.getTopicById(topicDto.aggregateId)
        reread.name == TOPIC_NAME_ORIGINAL
    }
}
