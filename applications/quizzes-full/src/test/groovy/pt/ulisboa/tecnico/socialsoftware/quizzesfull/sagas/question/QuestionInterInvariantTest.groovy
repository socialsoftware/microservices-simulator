package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.question

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Question
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.notification.handling.QuestionEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class QuestionInterInvariantTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    QuestionEventHandling questionEventHandling

    // ─── TOPICS_EXIST — UpdateTopicEvent ──────────────────────────────────────

    def "question reflects UpdateTopicEvent"() {
        given:
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        createExecution(course.aggregateId, "SE-2025", "2025/2026")
        def topic = createTopic(course.aggregateId, "Original Topic Name")
        def question = createQuestion(course.aggregateId, [topic.aggregateId], "Q1 Title", "Q1 Content")

        when: 'topic name is updated, publishing UpdateTopicEvent'
        def topicDto = new TopicDto()
        topicDto.aggregateId = topic.aggregateId
        topicDto.name = "Updated Topic Name"
        topicFunctionalities.updateTopic(topicDto)

        and: 'question polls for update topic events'
        questionEventHandling.handleUpdateTopicEvents()

        then: 'cached topic name in question is updated'
        def uow = unitOfWorkService.createUnitOfWork("check")
        def updatedQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(question.aggregateId, uow)
        updatedQuestion.topics.any { it.topicAggregateId == topic.aggregateId && it.topicName == "Updated Topic Name" }
    }

    def "question ignores UpdateTopicEvent for unrelated topic"() {
        given:
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        createExecution(course.aggregateId, "SE-2025", "2025/2026")
        def topic1 = createTopic(course.aggregateId, "Topic One")
        def topic2 = createTopic(course.aggregateId, "Topic Two")
        def question = createQuestion(course.aggregateId, [topic1.aggregateId], "Q1 Title", "Q1 Content")

        when: 'an unrelated topic is updated'
        def topicDto = new TopicDto()
        topicDto.aggregateId = topic2.aggregateId
        topicDto.name = "Updated Topic Two"
        topicFunctionalities.updateTopic(topicDto)

        and: 'question polls for update topic events'
        questionEventHandling.handleUpdateTopicEvents()

        then: 'cached topic name for topic1 in question is unchanged'
        def uow = unitOfWorkService.createUnitOfWork("check")
        def unchanged = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(question.aggregateId, uow)
        unchanged.topics.any { it.topicAggregateId == topic1.aggregateId && it.topicName == "Topic One" }
    }

    // ─── TOPICS_EXIST — DeleteTopicEvent ──────────────────────────────────────

    def "question removes topic on DeleteTopicEvent"() {
        given:
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        createExecution(course.aggregateId, "SE-2025", "2025/2026")
        def topic = createTopic(course.aggregateId, "Topic To Delete")
        def question = createQuestion(course.aggregateId, [topic.aggregateId], "Q1 Title", "Q1 Content")

        when: 'topic is deleted, publishing DeleteTopicEvent'
        topicFunctionalities.deleteTopic(topic.aggregateId)

        and: 'question polls for delete topic events'
        questionEventHandling.handleDeleteTopicEvents()

        then: 'topic is removed from question'
        def uow = unitOfWorkService.createUnitOfWork("check")
        def updatedQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(question.aggregateId, uow)
        updatedQuestion.topics.every { it.topicAggregateId != topic.aggregateId }
    }

    def "question ignores DeleteTopicEvent for unrelated topic"() {
        given:
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        createExecution(course.aggregateId, "SE-2025", "2025/2026")
        def topic1 = createTopic(course.aggregateId, "Topic One")
        def topic2 = createTopic(course.aggregateId, "Topic Two")
        def question = createQuestion(course.aggregateId, [topic1.aggregateId], "Q1 Title", "Q1 Content")

        when: 'an unrelated topic is deleted'
        topicFunctionalities.deleteTopic(topic2.aggregateId)

        and: 'question polls for delete topic events'
        questionEventHandling.handleDeleteTopicEvents()

        then: 'topic1 is still in question'
        def uow = unitOfWorkService.createUnitOfWork("check")
        def unchanged = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(question.aggregateId, uow)
        unchanged.topics.any { it.topicAggregateId == topic1.aggregateId }
    }
}
