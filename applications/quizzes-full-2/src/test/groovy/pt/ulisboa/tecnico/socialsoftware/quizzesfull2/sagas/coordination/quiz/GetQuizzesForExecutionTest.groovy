package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.quiz

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

@DataJpaTest
@Transactional
@Import(GetQuizzesForExecutionTest.LocalBeanConfiguration)
class GetQuizzesForExecutionTest extends QuizzesFull2SpockTest {

    public static final String SECOND_QUIZ_TITLE = "Graph quiz"

    def "getQuizzesForExecution: success"() {
        // Spec: plan.md §6 Quiz — GetQuizzesForExecution(executionAggregateId)
        given: 'an execution with two quizzes'
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def firstAggregateId = createQuiz(executionAggregateId)
        def secondAggregateId = createQuiz(executionAggregateId, SECOND_QUIZ_TITLE)

        when:
        def result = quizFunctionalities.getQuizzesForExecution(executionAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.size() == 2
        result.collect { it.aggregateId } as Set == [firstAggregateId, secondAggregateId] as Set
        result.find { it.aggregateId == secondAggregateId }.title == SECOND_QUIZ_TITLE
        sagaStateOf(firstAggregateId) == GenericSagaState.NOT_IN_SAGA
        sagaStateOf(secondAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
