package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.topic

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetTopicsByCourseIdTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String TOPIC_NAME_1 = "Algorithms"
    public static final String TOPIC_NAME_2 = "Data Structures"

    def courseDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
    }

    def "getTopicsByCourseId: returns all topics for a course"() {
        given:
        createTopic(courseDto.aggregateId, TOPIC_NAME_1)
        createTopic(courseDto.aggregateId, TOPIC_NAME_2)

        when:
        def result = topicFunctionalities.getTopicsByCourseId(courseDto.aggregateId)

        then:
        result.size() == 2
        result*.name.containsAll([TOPIC_NAME_1, TOPIC_NAME_2])
        result*.courseId.every { it == courseDto.aggregateId }
    }

    def "getTopicsByCourseId: returns empty list for unknown courseId"() {
        when:
        def result = topicFunctionalities.getTopicsByCourseId(999)

        then:
        result.isEmpty()
    }
}
