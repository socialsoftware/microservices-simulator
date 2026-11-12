package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.topic

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto

@DataJpaTest
@Transactional
@Import(CreateTopicTest.LocalBeanConfiguration)
class CreateTopicTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_COURSE_AGGREGATE_ID = 999999

    def "createTopic: success"() {
        // Spec: plan.md §3 Topic — CreateTopic(courseAggregateId, name)
        given: 'an existing course'
        def courseAggregateId = createCourse()
        def topicDto = new TopicDto()
        topicDto.setName(TOPIC_NAME)
        topicDto.setCourseAggregateId(courseAggregateId)

        when:
        def result = topicFunctionalities.createTopic(topicDto)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.aggregateId != null
        result.name == TOPIC_NAME
        result.courseAggregateId == courseAggregateId
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createTopic: aborts when the course prerequisite does not exist"() {
        // Spec: plan.md §3 Topic — cross-aggregate prerequisite (P4a): getCourseStep fetches the
        // course and throws if it does not exist, so no explicit guard is written
        given:
        def topicDto = new TopicDto()
        topicDto.setName(TOPIC_NAME)
        topicDto.setCourseAggregateId(NONEXISTENT_COURSE_AGGREGATE_ID)

        when:
        topicFunctionalities.createTopic(topicDto)

        then:
        thrown(SimulatorException)
    }

    // Semantic-lock acquisition: CreateTopicFunctionalitySagas has no setSemanticLock step — the
    // create step brings the aggregate into existence (sagas.md § Create Functionality Sagas), so
    // there is no prior state to lock, and getCourseStep is a plain upstream read.

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
