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
@Import(GetUserExecutionsTest.LocalBeanConfiguration)
class GetUserExecutionsTest extends QuizzesFull2SpockTest {

    def "getUserExecutions: success"() {
        // Spec: plan.md §4 Execution — GetUserExecutions(userAggregateId)
        given: 'the user is enrolled in one of two executions'
        def courseAggregateId = createCourse()
        def enrolledAggregateId = createExecution(courseAggregateId, EXECUTION_ACRONYM)
        def otherAggregateId = createExecution(courseAggregateId, "SE-02")
        def studentAggregateId = createActiveUser(EXECUTION_STUDENT_USER_NAME, EXECUTION_STUDENT_USER_USERNAME)
        def otherStudentAggregateId = createActiveUser()
        enrollStudentInExecution(enrolledAggregateId, studentAggregateId)
        enrollStudentInExecution(otherAggregateId, otherStudentAggregateId)

        when:
        def result = executionFunctionalities.getUserExecutions(studentAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.collect { it.aggregateId } == [enrolledAggregateId]
        result[0].acronym == EXECUTION_ACRONYM
        sagaStateOf(enrolledAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
