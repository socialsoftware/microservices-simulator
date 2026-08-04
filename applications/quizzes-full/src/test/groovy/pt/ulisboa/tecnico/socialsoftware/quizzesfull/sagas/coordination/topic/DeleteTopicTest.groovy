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
class DeleteTopicTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String TOPIC_NAME_1 = "Algorithms"

    def courseDto
    def topicDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        topicDto = createTopic(courseDto.aggregateId, TOPIC_NAME_1)
    }

    def "deleteTopic: success"() {
        when:
        topicFunctionalities.deleteTopic(topicDto.aggregateId)

        then: 'orchestration outcome only — persisted deletion is asserted in TopicServiceTest'
        noExceptionThrown()
    }

}
