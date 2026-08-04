package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.topic

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteTopicEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateTopicEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.Topic
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto

@DataJpaTest
@Transactional
@Import(TopicServiceTest.LocalBeanConfiguration)
class TopicServiceTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999
    public static final String UPDATED_TOPIC_NAME = "Advanced Algorithms"

    @Autowired
    EventService eventService

    def "getTopicById: reads back the persisted topic through a fresh UnitOfWork"() {
        // Spec: plan.md §3 Topic — fields name, courseAggregateId
        given:
        def courseAggregateId = createCourse()
        def topicAggregateId = createTopic(courseAggregateId)

        when:
        flushAndClear()
        def result = topicService.getTopicById(topicAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == topicAggregateId
        result.name == TOPIC_NAME
        result.courseAggregateId == courseAggregateId
        result.version != null
    }

    def "getTopicById: unknown aggregate id is not found"() {
        // Spec: plan.md §3 Topic — Path A (aggregateLoadAndRegisterRead)
        when:
        topicService.getTopicById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "getTopicsByCourse: returns every topic of the course with its own state"() {
        // Spec: plan.md §3 Topic — GetTopicsByCourse(courseAggregateId)
        given:
        def courseAggregateId = createCourse()
        def firstAggregateId = createTopic(courseAggregateId, TOPIC_NAME)
        def secondAggregateId = createTopic(courseAggregateId, "Concurrency")

        when:
        flushAndClear()
        def result = topicService.getTopicsByCourse(courseAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.size() == 2
        def first = result.find { it.aggregateId == firstAggregateId }
        first.name == TOPIC_NAME
        first.courseAggregateId == courseAggregateId
        def second = result.find { it.aggregateId == secondAggregateId }
        second.name == "Concurrency"
        second.courseAggregateId == courseAggregateId
    }

    def "getTopicsByCourse: excludes topics belonging to another course"() {
        // Spec: plan.md §3 Topic — GetTopicsByCourse(courseAggregateId) filters on courseAggregateId
        given:
        def courseAggregateId = createCourse()
        def otherCourseAggregateId = createCourse("Distributed Systems", CourseType.EXTERNAL)
        def topicAggregateId = createTopic(courseAggregateId)
        createTopic(otherCourseAggregateId, "Consensus")

        when:
        flushAndClear()
        def result = topicService.getTopicsByCourse(courseAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.collect { it.aggregateId } == [topicAggregateId]
    }

    def "getTopicsByCourse: returns an empty list when the course has no topic"() {
        // Spec: plan.md §3 Topic — GetTopicsByCourse(courseAggregateId)
        given:
        def courseAggregateId = createCourse()

        when:
        def result = topicService.getTopicsByCourse(courseAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.isEmpty()
    }

    def "createTopic: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §3 Topic — CreateTopic(courseAggregateId, name)
        given:
        def courseAggregateId = createCourse()
        def topicDto = new TopicDto()
        topicDto.setName(TOPIC_NAME)
        topicDto.setCourseAggregateId(courseAggregateId)

        when:
        def result = topicService.createTopic(topicDto,
                unitOfWorkService.createUnitOfWork("createTopic"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        result.aggregateId != null
        flushAndClear()
        def readBack = topicService.getTopicById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == TOPIC_NAME
        readBack.courseAggregateId == courseAggregateId
    }

    def "updateTopic: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §3 Topic — UpdateTopic(topicAggregateId, name)
        given:
        def courseAggregateId = createCourse()
        def topicAggregateId = createTopic(courseAggregateId)

        when:
        topicService.updateTopic(topicAggregateId, UPDATED_TOPIC_NAME,
                unitOfWorkService.createUnitOfWork("updateTopic"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = topicService.getTopicById(topicAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == UPDATED_TOPIC_NAME
        readBack.courseAggregateId == courseAggregateId
    }

    def "updateTopic: unknown aggregate id is not found"() {
        // Spec: plan.md §3 Topic — UpdateTopic(topicAggregateId, name); Path A (aggregateLoadAndRegisterRead)
        when:
        topicService.updateTopic(NONEXISTENT_AGGREGATE_ID, UPDATED_TOPIC_NAME,
                unitOfWorkService.createUnitOfWork("updateTopic"))

        then:
        thrown(SimulatorException)
    }

    def "deleteTopic: soft-deletes the topic, keeping its fields"() {
        // Spec: plan.md §3 Topic — DeleteTopic(topicAggregateId) soft-deletes the topic
        given:
        def courseAggregateId = createCourse()
        def topicAggregateId = createTopic(courseAggregateId)

        when:
        topicService.deleteTopic(topicAggregateId,
                unitOfWorkService.createUnitOfWork("deleteTopic"))

        then: 'read back off a cleared persistence context, through the deleted-version load path'
        flushAndClear()
        def readBack = (Topic) unitOfWorkService.aggregateDeletedLoad(topicAggregateId)
        readBack.state == Aggregate.AggregateState.DELETED
        readBack.name == TOPIC_NAME
        readBack.courseAggregateId == courseAggregateId
    }

    def "deleteTopic: the deleted topic is no longer visible to the non-deleted load path"() {
        // Spec: plan.md §3 Topic — DeleteTopic(topicAggregateId) soft-deletes the topic
        given:
        def courseAggregateId = createCourse()
        def topicAggregateId = createTopic(courseAggregateId)
        topicService.deleteTopic(topicAggregateId,
                unitOfWorkService.createUnitOfWork("deleteTopic"))

        when:
        flushAndClear()
        topicService.getTopicById(topicAggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "deleteTopic: unknown aggregate id is not found"() {
        // Spec: plan.md §3 Topic — DeleteTopic(topicAggregateId); Path A (aggregateLoadAndRegisterRead)
        when:
        topicService.deleteTopic(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("deleteTopic"))

        then:
        thrown(SimulatorException)
    }

    def "updateTopic publishes UpdateTopicEvent with correct payload"() {
        // Spec: plan.md §3 Topic — events published: UpdateTopicEvent; payload topicAggregateId, topicName
        given:
        def courseAggregateId = createCourse()
        def topicAggregateId = createTopic(courseAggregateId)

        when:
        topicService.updateTopic(topicAggregateId, UPDATED_TOPIC_NAME,
                unitOfWorkService.createUnitOfWork("updateTopic"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof UpdateTopicEvent }
        events.size() == 1
        def event = events[0] as UpdateTopicEvent
        event.publisherAggregateId == topicAggregateId
        event.topicAggregateId == topicAggregateId
        event.topicName == UPDATED_TOPIC_NAME
    }

    def "deleteTopic publishes DeleteTopicEvent with correct payload"() {
        // Spec: plan.md §3 Topic — events published: DeleteTopicEvent; payload topicAggregateId
        given:
        def courseAggregateId = createCourse()
        def topicAggregateId = createTopic(courseAggregateId)

        when:
        topicService.deleteTopic(topicAggregateId,
                unitOfWorkService.createUnitOfWork("deleteTopic"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof DeleteTopicEvent }
        events.size() == 1
        def event = events[0] as DeleteTopicEvent
        event.publisherAggregateId == topicAggregateId
        event.topicAggregateId == topicAggregateId
    }

    def "createTopic publishes no event"() {
        // Spec: plan.md §3 Topic — only UpdateTopicEvent and DeleteTopicEvent are published
        given:
        def courseAggregateId = createCourse()
        def countBefore = eventService.getAllEvents().size()
        def topicDto = new TopicDto()
        topicDto.setName("Concurrency")
        topicDto.setCourseAggregateId(courseAggregateId)

        when:
        topicService.createTopic(topicDto, unitOfWorkService.createUnitOfWork("createTopic"))

        then:
        eventService.getAllEvents().size() == countBefore
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
