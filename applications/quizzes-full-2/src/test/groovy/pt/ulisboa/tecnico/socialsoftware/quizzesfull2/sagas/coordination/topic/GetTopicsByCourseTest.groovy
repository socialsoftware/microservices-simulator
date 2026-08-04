package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.topic

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

@DataJpaTest
@Transactional
@Import(GetTopicsByCourseTest.LocalBeanConfiguration)
class GetTopicsByCourseTest extends QuizzesFull2SpockTest {

    def "getTopicsByCourse: success"() {
        // Spec: plan.md §3 Topic — GetTopicsByCourse(courseAggregateId)
        given: 'a course with two topics'
        def courseAggregateId = createCourse()
        def firstAggregateId = createTopic(courseAggregateId, TOPIC_NAME)
        def secondAggregateId = createTopic(courseAggregateId, "Concurrency")

        when:
        def result = topicFunctionalities.getTopicsByCourse(courseAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.size() == 2
        result.collect { it.aggregateId } as Set == [firstAggregateId, secondAggregateId] as Set
        result.find { it.aggregateId == secondAggregateId }.name == "Concurrency"
        sagaStateOf(firstAggregateId) == GenericSagaState.NOT_IN_SAGA
        sagaStateOf(secondAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
