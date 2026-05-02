package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.topic

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.Topic
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

    def "updateTopic: success — name is persisted"() {
        given:
        TopicDto update = new TopicDto()
        update.aggregateId = topicDto.aggregateId
        update.name = TOPIC_NAME_UPDATED

        when:
        topicFunctionalities.updateTopic(update)

        then:
        def uow = unitOfWorkService.createUnitOfWork("verify")
        Topic fetched = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicDto.aggregateId, uow)
        fetched.name == TOPIC_NAME_UPDATED
    }

    def "updateTopic: null name throws exception"() {
        given:
        TopicDto update = new TopicDto()
        update.aggregateId = topicDto.aggregateId
        update.name = null

        when:
        topicFunctionalities.updateTopic(update)

        then:
        thrown(QuizzesFullException)
    }
}
