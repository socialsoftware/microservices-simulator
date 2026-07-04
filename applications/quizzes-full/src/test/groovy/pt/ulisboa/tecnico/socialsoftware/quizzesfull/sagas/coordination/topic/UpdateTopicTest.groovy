package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.topic

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateTopicTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String TOPIC_NAME_ORIGINAL = "Algorithms"
    public static final String TOPIC_NAME_UPDATED  = "Advanced Algorithms"

    def courseDto
    def topicDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        topicDto = createTopic(courseDto.aggregateId, TOPIC_NAME_ORIGINAL)
    }

    def "updateTopic: success"() {
        // Spec: plan.md §3 Topic — UpdateTopic; orchestration outcome only, persistence in TopicServiceTest.
        given:
        TopicDto update = new TopicDto()
        update.aggregateId = topicDto.aggregateId
        update.name = TOPIC_NAME_UPDATED

        when:
        topicFunctionalities.updateTopic(update)

        then:
        noExceptionThrown()
        sagaStateOf(topicDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

}
