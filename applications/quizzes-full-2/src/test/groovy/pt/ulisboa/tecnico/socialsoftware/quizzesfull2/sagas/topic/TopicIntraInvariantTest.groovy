package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.topic

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.sagas.SagaTopic

@DataJpaTest
@Transactional
@Import(TopicIntraInvariantTest.LocalBeanConfiguration)
class TopicIntraInvariantTest extends QuizzesFull2SpockTest {

    def "create topic"() {
        // Spec: plan.md §3 Topic — CreateTopic(courseAggregateId, name); fields name, courseAggregateId
        when:
        def topic = new SagaTopic(TOPIC_AGGREGATE_ID, TOPIC_NAME, COURSE_AGGREGATE_ID)
        topic.verifyInvariants()

        then:
        topic.aggregateId == TOPIC_AGGREGATE_ID
        topic.name == TOPIC_NAME
        topic.courseAggregateId == COURSE_AGGREGATE_ID
        topic.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    // Topic has no P1 rule to violate: neither §3.1 nor §3.2 of the domain model names one for this
    // aggregate, and its immutable Course reference is a Java `final` field, which testing.md § T1
    // excludes from coverage.

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
