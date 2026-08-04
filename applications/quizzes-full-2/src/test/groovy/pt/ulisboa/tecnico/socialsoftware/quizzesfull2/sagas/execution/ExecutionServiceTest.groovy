package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.execution

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

@DataJpaTest
@Transactional
@Import(ExecutionServiceTest.LocalBeanConfiguration)
class ExecutionServiceTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999
    public static final String OTHER_EXECUTION_ACRONYM = "SE-02"

    def "getExecutionById: reads back the persisted execution through a fresh UnitOfWork"() {
        // Spec: plan.md §4 Execution — GetExecutionById(executionAggregateId)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)

        when:
        flushAndClear()
        def result = executionService.getExecutionById(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == executionAggregateId
        result.courseAggregateId == courseAggregateId
        result.courseName == COURSE_NAME
        result.courseType == COURSE_TYPE
        result.acronym == EXECUTION_ACRONYM
        result.academicTerm == EXECUTION_ACADEMIC_TERM
        result.endDate == EXECUTION_END_DATE
        result.students.isEmpty()
        result.version != null
    }

    def "getExecutionById: unknown aggregate id is not found"() {
        // Spec: plan.md §4 Execution — Path A (aggregateLoadAndRegisterRead)
        when:
        executionService.getExecutionById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "getExecutions: returns every execution with its own state"() {
        // Spec: plan.md §4 Execution — GetExecutions()
        given:
        def courseAggregateId = createCourse()
        def firstAggregateId = createExecution(courseAggregateId, EXECUTION_ACRONYM)
        def secondAggregateId = createExecution(courseAggregateId, OTHER_EXECUTION_ACRONYM)

        when:
        flushAndClear()
        def result = executionService.getExecutions(unitOfWorkService.createUnitOfWork("check"))

        then:
        result.size() == 2
        def first = result.find { it.aggregateId == firstAggregateId }
        first.acronym == EXECUTION_ACRONYM
        first.courseAggregateId == courseAggregateId
        def second = result.find { it.aggregateId == secondAggregateId }
        second.acronym == OTHER_EXECUTION_ACRONYM
        second.academicTerm == EXECUTION_ACADEMIC_TERM
    }

    def "getExecutions: returns an empty list when no execution exists"() {
        // Spec: plan.md §4 Execution — GetExecutions()
        when:
        def result = executionService.getExecutions(unitOfWorkService.createUnitOfWork("check"))

        then:
        result.isEmpty()
    }

    def "getUserExecutions: returns the executions the user is enrolled in"() {
        // Spec: plan.md §4 Execution — GetUserExecutions(userAggregateId)
        given:
        def courseAggregateId = createCourse()
        def firstAggregateId = createExecution(courseAggregateId, EXECUTION_ACRONYM)
        def secondAggregateId = createExecution(courseAggregateId, OTHER_EXECUTION_ACRONYM)
        enrollStudentInExecution(firstAggregateId, EXECUTION_STUDENT_USER_AGGREGATE_ID)
        enrollStudentInExecution(secondAggregateId, EXECUTION_STUDENT_USER_AGGREGATE_ID)

        when:
        flushAndClear()
        def result = executionService.getUserExecutions(EXECUTION_STUDENT_USER_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.size() == 2
        result.collect { it.aggregateId } as Set == [firstAggregateId, secondAggregateId] as Set
        result.every { it.students.collect { student -> student.userAggregateId } == [EXECUTION_STUDENT_USER_AGGREGATE_ID] }
    }

    def "getUserExecutions: excludes executions the user is not enrolled in"() {
        // Spec: plan.md §4 Execution — GetUserExecutions(userAggregateId) filters on the enrolled student
        given:
        def courseAggregateId = createCourse()
        def enrolledAggregateId = createExecution(courseAggregateId, EXECUTION_ACRONYM)
        def otherAggregateId = createExecution(courseAggregateId, OTHER_EXECUTION_ACRONYM)
        enrollStudentInExecution(enrolledAggregateId, EXECUTION_STUDENT_USER_AGGREGATE_ID)
        enrollStudentInExecution(otherAggregateId, EXECUTION_STUDENT_USER_AGGREGATE_ID_2)

        when:
        flushAndClear()
        def result = executionService.getUserExecutions(EXECUTION_STUDENT_USER_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.collect { it.aggregateId } == [enrolledAggregateId]
    }

    def "getUserExecutions: returns an empty list when the user is enrolled nowhere"() {
        // Spec: plan.md §4 Execution — GetUserExecutions(userAggregateId)
        given:
        def courseAggregateId = createCourse()
        createExecution(courseAggregateId)

        when:
        flushAndClear()
        def result = executionService.getUserExecutions(EXECUTION_STUDENT_USER_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.isEmpty()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
