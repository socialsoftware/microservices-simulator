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
@Import(GetQuestionByIdTest.LocalBeanConfiguration)
class GetQuestionByIdTest extends QuizzesFull2SpockTest {

    def "getQuestionById: success"() {
        // Spec: plan.md §5 Question — GetQuestionById(questionAggregateId)
        given: 'a question exists'
        def courseAggregateId = createCourse()
        def questionAggregateId = createQuestion(courseAggregateId)

        when:
        def result = questionFunctionalities.getQuestionById(questionAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.aggregateId == questionAggregateId
        result.courseAggregateId == courseAggregateId
        result.title == QUESTION_TITLE
        result.content == QUESTION_CONTENT
        sagaStateOf(questionAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
