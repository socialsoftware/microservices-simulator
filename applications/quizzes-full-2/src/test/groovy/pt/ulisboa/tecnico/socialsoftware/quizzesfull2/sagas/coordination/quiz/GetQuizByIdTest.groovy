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
@Import(GetQuizByIdTest.LocalBeanConfiguration)
class GetQuizByIdTest extends QuizzesFull2SpockTest {

    def "getQuizById: success"() {
        // Spec: plan.md §6 Quiz — GetQuizById(quizAggregateId)
        given: 'a quiz exists'
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId)

        when:
        def result = quizFunctionalities.getQuizById(quizAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.aggregateId == quizAggregateId
        result.title == QUIZ_TITLE
        result.quizType == QUIZ_TYPE
        result.executionAggregateId == executionAggregateId
        sagaStateOf(quizAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
