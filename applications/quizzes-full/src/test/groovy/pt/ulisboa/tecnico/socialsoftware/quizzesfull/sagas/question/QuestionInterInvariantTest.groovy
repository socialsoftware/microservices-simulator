package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.question

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Question
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.notification.handling.QuestionEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.InterInvariantTestBase

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class QuestionInterInvariantTest extends InterInvariantTestBase {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    QuestionEventHandling questionEventHandling

    def setup() {
        buildFixture([Stage.QUESTION] as Set)
    }

    // ─── TOPICS_EXIST — UpdateTopicEvent ──────────────────────────────────────

    def "question updates topicName on UpdateTopicEvent"() {
        when: 'topic name is updated, publishing UpdateTopicEvent'
        def topicDto = new TopicDto()
        topicDto.aggregateId = topicId
        topicDto.name = "Updated Topic Name"
        topicFunctionalities.updateTopic(topicDto)

        and: 'question polls for update topic events'
        questionEventHandling.handleUpdateTopicEvents()

        then: 'cached topic name in question is updated'
        def updatedQuestion = loadForCheck(questionId, Question)
        updatedQuestion.topics.any { it.topicAggregateId == topicId && it.topicName == "Updated Topic Name" }
    }

    def "question ignores UpdateTopicEvent for unrelated topic"() {
        given:
        def topic2 = createTopic(courseId, "Topic Two")

        when: 'an unrelated topic is updated'
        def topicDto = new TopicDto()
        topicDto.aggregateId = topic2.aggregateId
        topicDto.name = "Updated Topic Two"
        topicFunctionalities.updateTopic(topicDto)

        and: 'question polls for update topic events'
        questionEventHandling.handleUpdateTopicEvents()

        then: 'cached topic name for topic1 in question is unchanged'
        def unchanged = loadForCheck(questionId, Question)
        unchanged.topics.any { it.topicAggregateId == topicId && it.topicName == "Topic A" }
    }

    // ─── TOPICS_EXIST — DeleteTopicEvent ──────────────────────────────────────

    def "question removes topic on DeleteTopicEvent"() {
        when: 'topic is deleted, publishing DeleteTopicEvent'
        topicFunctionalities.deleteTopic(topicId)

        and: 'question polls for delete topic events'
        questionEventHandling.handleDeleteTopicEvents()

        then: 'topic is removed from question'
        def updatedQuestion = loadForCheck(questionId, Question)
        updatedQuestion.topics.every { it.topicAggregateId != topicId }
    }

    def "question ignores DeleteTopicEvent for unrelated topic"() {
        given:
        def topic2 = createTopic(courseId, "Topic Two")

        when: 'an unrelated topic is deleted'
        topicFunctionalities.deleteTopic(topic2.aggregateId)

        and: 'question polls for delete topic events'
        questionEventHandling.handleDeleteTopicEvents()

        then: 'topic1 is still in question'
        def unchanged = loadForCheck(questionId, Question)
        unchanged.topics.any { it.topicAggregateId == topicId }
    }
}
