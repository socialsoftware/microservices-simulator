package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.question

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

@DataJpaTest
@Transactional
@Import(GetQuestionsByCourseTest.LocalBeanConfiguration)
class GetQuestionsByCourseTest extends QuizzesFull2SpockTest {

    def "getQuestionsByCourse: success"() {
        // Spec: plan.md §5 Question — GetQuestionsByCourse(courseAggregateId)
        given: 'a course with two questions'
        def courseAggregateId = createCourse()
        def firstAggregateId = createQuestion(courseAggregateId)
        def secondAggregateId = createQuestion(courseAggregateId, "Graph traversal",
                "What is the complexity of breadth-first search?")

        when:
        def result = questionFunctionalities.getQuestionsByCourse(courseAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.size() == 2
        result.collect { it.aggregateId } as Set == [firstAggregateId, secondAggregateId] as Set
        result.find { it.aggregateId == secondAggregateId }.title == "Graph traversal"
        sagaStateOf(firstAggregateId) == GenericSagaState.NOT_IN_SAGA
        sagaStateOf(secondAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
