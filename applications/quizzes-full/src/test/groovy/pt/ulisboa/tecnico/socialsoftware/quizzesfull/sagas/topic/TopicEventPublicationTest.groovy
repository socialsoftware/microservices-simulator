package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.topic

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteTopicEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateTopicEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicCourse
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class TopicEventPublicationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String TOPIC_NAME_1 = "Algorithms"
    public static final String TOPIC_NAME_UPDATED = "Advanced Algorithms"

    @Autowired
    EventService eventService

    def "updateTopic publishes UpdateTopicEvent with correct payload"() {
        // Spec: plan.md §3 Topic — events published by UpdateTopic
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def topic = createTopic(courseDto.aggregateId, TOPIC_NAME_1)
        def update = new TopicDto()
        update.aggregateId = topic.aggregateId
        update.name = TOPIC_NAME_UPDATED

        when:
        topicService.updateTopic(update, unitOfWorkService.createUnitOfWork("updateTopic"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof UpdateTopicEvent }
        events.size() == 1
        def event = events[0] as UpdateTopicEvent
        event.publisherAggregateId == topic.aggregateId
        event.topicName == TOPIC_NAME_UPDATED
    }

    def "deleteTopic publishes DeleteTopicEvent with correct payload"() {
        // Spec: plan.md §3 Topic — events published by DeleteTopic
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def topic = createTopic(courseDto.aggregateId, TOPIC_NAME_1)

        when:
        topicService.deleteTopic(topic.aggregateId, unitOfWorkService.createUnitOfWork("deleteTopic"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof DeleteTopicEvent }
        events.size() == 1
        def event = events[0] as DeleteTopicEvent
        event.publisherAggregateId == topic.aggregateId
    }

    def "createTopic does not publish any event"() {
        // Negative case: CreateTopic has no events-published entry in plan.md §3 Topic
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def input = new TopicDto()
        input.name = TOPIC_NAME_1
        def countBefore = eventService.getAllEvents().size()

        when:
        topicService.createTopic(input, new TopicCourse(courseDto),
                unitOfWorkService.createUnitOfWork("createTopic"))

        then:
        eventService.getAllEvents().size() == countBefore
    }
}
