package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.execution

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

@DataJpaTest
@Transactional
@Import(GetExecutionByIdTest.LocalBeanConfiguration)
class GetExecutionByIdTest extends QuizzesFull2SpockTest {

    def "getExecutionById: success"() {
        // Spec: plan.md §4 Execution — GetExecutionById(executionAggregateId)
        given: 'an execution exists'
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)

        when:
        def result = executionFunctionalities.getExecutionById(executionAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.aggregateId == executionAggregateId
        result.courseAggregateId == courseAggregateId
        result.acronym == EXECUTION_ACRONYM
        result.academicTerm == EXECUTION_ACADEMIC_TERM
        sagaStateOf(executionAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
