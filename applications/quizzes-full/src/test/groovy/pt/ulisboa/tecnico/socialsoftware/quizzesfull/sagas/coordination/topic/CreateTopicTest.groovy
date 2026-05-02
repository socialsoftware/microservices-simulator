package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.topic

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateTopicTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String TOPIC_NAME_1 = "Algorithms"
    public static final String TOPIC_NAME_2 = "Data Structures"

    def courseDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
    }

    def "createTopic: success"() {
        when:
        TopicDto result = createTopic(courseDto.aggregateId, TOPIC_NAME_1)

        then:
        result.aggregateId != null
        result.name == TOPIC_NAME_1
        result.courseId == courseDto.aggregateId
    }

    def "createTopic: success with second topic on same course"() {
        given:
        createTopic(courseDto.aggregateId, TOPIC_NAME_1)

        when:
        TopicDto result = createTopic(courseDto.aggregateId, TOPIC_NAME_2)

        then:
        result.aggregateId != null
        result.name == TOPIC_NAME_2
        result.courseId == courseDto.aggregateId
    }
}
