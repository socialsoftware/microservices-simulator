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
class CreateTopicCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String TOPIC_NAME_1 = "Algorithms"

    def courseDto

    def setup() {
        loadBehaviorScripts()
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "createTopic: fault on createTopicStep compensates the lock acquired by getCourseStep"() {
        // getCourseStep has already set CourseSagaState.READ_COURSE and registered its
        // compensation by the time createTopicStep's injected fault fires. The Topic doesn't
        // exist yet at this point in the saga, so the released lock is on Course.
        given:
        TopicDto topicDto = new TopicDto()
        topicDto.name = TOPIC_NAME_1

        when:
        topicFunctionalities.createTopic(courseDto.aggregateId, topicDto)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the Course semantic lock back to NOT_IN_SAGA'
        sagaStateOf(courseDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: no topic was created on the course'
        topicFunctionalities.getTopicsByCourseId(courseDto.aggregateId).isEmpty()
    }
}
