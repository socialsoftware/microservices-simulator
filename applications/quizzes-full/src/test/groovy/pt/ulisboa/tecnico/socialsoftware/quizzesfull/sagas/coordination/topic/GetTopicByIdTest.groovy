package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.topic

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetTopicByIdTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String TOPIC_NAME_1 = "Algorithms"

    def courseDto
    def topicDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        topicDto = createTopic(courseDto.aggregateId, TOPIC_NAME_1)
    }

    def "getTopicById: success"() {
        when:
        TopicDto result = topicFunctionalities.getTopicById(topicDto.aggregateId)

        then:
        result.aggregateId == topicDto.aggregateId
        result.name == TOPIC_NAME_1
        result.courseId == courseDto.aggregateId
    }

    def "getTopicById: topic not found"() {
        when:
        topicFunctionalities.getTopicById(999)

        then:
        thrown(SimulatorException)
    }
}
