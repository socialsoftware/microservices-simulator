package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.topic

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType

@DataJpaTest
@Transactional
@Import(TopicServiceTest.LocalBeanConfiguration)
class TopicServiceTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999

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

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
