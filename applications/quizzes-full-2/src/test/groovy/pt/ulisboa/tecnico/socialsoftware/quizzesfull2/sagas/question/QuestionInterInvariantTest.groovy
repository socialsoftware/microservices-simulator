package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.question

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionTopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.notification.handling.QuestionEventHandling

@DataJpaTest
@Transactional
@Import(QuestionInterInvariantTest.LocalBeanConfiguration)
class QuestionInterInvariantTest extends QuizzesFull2SpockTest {

    public static final String TOPIC_A_NAME = "Sorting"
    public static final String TOPIC_B_NAME = "Graphs"
    public static final String UPDATED_TOPIC_NAME = "Sorting and Searching"

    @Autowired
    QuestionEventHandling questionEventHandling

    def "question updates the cached topic name on UpdateTopicEvent"() {
        // Spec: plan.md §5 Question — subscribed UpdateTopicEvent; grouping §2 Question/Topic row
        // caches (topicName, topicVersion). Payload field: topicName.
        given:
        def courseAggregateId = createCourse()
        def topicAggregateId = createTopic(courseAggregateId, TOPIC_A_NAME)
        def questionAggregateId = createQuestion(courseAggregateId, QUESTION_TITLE, QUESTION_CONTENT,
                [topicAggregateId])
        def versionBefore = topicOf(questionAggregateId, topicAggregateId).topicVersion

        when: 'the topic is renamed'
        topicFunctionalities.updateTopic(topicAggregateId, UPDATED_TOPIC_NAME)

        and: 'the question polls for the event'
        questionEventHandling.handleUpdateTopicEvents()

        then: 'the cached topic carries the payload and advances past the published version'
        def topic = topicOf(questionAggregateId, topicAggregateId)
        topic.topicName == UPDATED_TOPIC_NAME
        topic.topicVersion > versionBefore
    }

    def "question ignores an UpdateTopicEvent for a topic it does not cache"() {
        // Spec: plan.md §5 Question — the subscription is anchored on each cached topic's aggregate id
        given:
        def courseAggregateId = createCourse()
        def topicAggregateId = createTopic(courseAggregateId, TOPIC_A_NAME)
        def unrelatedTopicAggregateId = createTopic(courseAggregateId, TOPIC_B_NAME)
        def questionAggregateId = createQuestion(courseAggregateId, QUESTION_TITLE, QUESTION_CONTENT,
                [topicAggregateId])
        def nameBefore = topicOf(questionAggregateId, topicAggregateId).topicName
        def versionBefore = topicOf(questionAggregateId, topicAggregateId).topicVersion

        when: 'a topic the question does not cache is renamed'
        topicFunctionalities.updateTopic(unrelatedTopicAggregateId, UPDATED_TOPIC_NAME)

        and: 'the question polls for the event'
        questionEventHandling.handleUpdateTopicEvents()

        then: 'the cached topic snapshot is untouched'
        def topic = topicOf(questionAggregateId, topicAggregateId)
        topic.topicName == nameBefore
        topic.topicVersion == versionBefore
    }

    def "question drops the cached topic on DeleteTopicEvent"() {
        // Spec: plan.md §3.2 — rule TOPICS_EXIST (Question), P2: no cached topic may reference a
        // deleted Topic. The question stays valid without that topic, so only the member is removed.
        given:
        def courseAggregateId = createCourse()
        def topicAggregateId = createTopic(courseAggregateId, TOPIC_A_NAME)
        def questionAggregateId = createQuestion(courseAggregateId, QUESTION_TITLE, QUESTION_CONTENT,
                [topicAggregateId])

        when: 'the topic is deleted'
        topicFunctionalities.deleteTopic(topicAggregateId)

        and: 'the question polls for the event'
        questionEventHandling.handleDeleteTopicEvents()

        then: 'the topic is gone from the question while the question itself survives'
        topicOf(questionAggregateId, topicAggregateId) == null
        questionService.getQuestionById(questionAggregateId,
                unitOfWorkService.createUnitOfWork("check")).topics.isEmpty()
    }

    def "question ignores a DeleteTopicEvent for another cached topic"() {
        // Spec: plan.md §3.2 — rule TOPICS_EXIST (Question) removes only the deleted topic
        given:
        def courseAggregateId = createCourse()
        def topicAggregateId = createTopic(courseAggregateId, TOPIC_A_NAME)
        def otherTopicAggregateId = createTopic(courseAggregateId, TOPIC_B_NAME)
        def questionAggregateId = createQuestion(courseAggregateId, QUESTION_TITLE, QUESTION_CONTENT,
                [topicAggregateId, otherTopicAggregateId])
        def nameBefore = topicOf(questionAggregateId, topicAggregateId).topicName

        when: 'the other cached topic is deleted'
        topicFunctionalities.deleteTopic(otherTopicAggregateId)

        and: 'the question polls for the event'
        questionEventHandling.handleDeleteTopicEvents()

        then: 'the surviving topic keeps its cached snapshot'
        def topic = topicOf(questionAggregateId, topicAggregateId)
        topic != null
        topic.topicName == nameBefore

        and: 'only the deleted topic left the question'
        topicOf(questionAggregateId, otherTopicAggregateId) == null
    }

    private QuestionTopicDto topicOf(Integer questionAggregateId, Integer topicAggregateId) {
        return questionService.getQuestionById(questionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
                .topics.find { it.topicAggregateId == topicAggregateId }
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
