package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.execution

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionStudent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.sagas.SagaExecution

@DataJpaTest
@Transactional
@Import(ExecutionIntraInvariantTest.LocalBeanConfiguration)
class ExecutionIntraInvariantTest extends QuizzesFull2SpockTest {

    private static SagaExecution anExecution() {
        return new SagaExecution(EXECUTION_AGGREGATE_ID, COURSE_AGGREGATE_ID, COURSE_NAME, COURSE_TYPE,
                EXECUTION_ACRONYM, EXECUTION_ACADEMIC_TERM, EXECUTION_END_DATE)
    }

    private static ExecutionStudent aStudent(Integer userAggregateId) {
        return new ExecutionStudent(userAggregateId, EXECUTION_STUDENT_USER_NAME, EXECUTION_STUDENT_USER_USERNAME,
                EXECUTION_STUDENT_USER_VERSION, true)
    }

    def "create execution"() {
        // Spec: plan.md §4 Execution — CreateExecution(courseAggregateId, acronym, academicTerm, endDate);
        // snapshot fields courseAggregateId, courseName, courseType (plan.md § snapshot table)
        when:
        def execution = anExecution()
        execution.verifyInvariants()

        then:
        execution.aggregateId == EXECUTION_AGGREGATE_ID
        execution.acronym == EXECUTION_ACRONYM
        execution.academicTerm == EXECUTION_ACADEMIC_TERM
        execution.endDate == EXECUTION_END_DATE
        execution.courseAggregateId == COURSE_AGGREGATE_ID
        execution.courseName == COURSE_NAME
        execution.courseType == COURSE_TYPE
        execution.students.isEmpty()
        execution.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    def "execution: REMOVE_NO_STUDENTS violation"() {
        // Spec: plan.md §3.1/§3.2 rule REMOVE_NO_STUDENTS — state == DELETED => students.isEmpty();
        // off-point of the collection-size boundary: one student on a deleted execution
        given:
        def execution = anExecution()
        execution.addStudent(aStudent(EXECUTION_STUDENT_USER_AGGREGATE_ID))
        execution.remove()

        when:
        execution.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.REMOVE_NO_STUDENTS
    }

    def "execution: REMOVE_NO_STUDENTS holds when the roster is cleared before deletion"() {
        // Spec: plan.md §3.2 rule REMOVE_NO_STUDENTS — on-point of the collection-size boundary:
        // zero students on a deleted execution, the state DeleteExecution must leave behind
        given:
        def execution = anExecution()
        execution.addStudent(aStudent(EXECUTION_STUDENT_USER_AGGREGATE_ID))
        execution.getStudents().clear()
        execution.remove()

        when:
        execution.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "execution: STUDENT_ALREADY_ENROLLED violation"() {
        // Spec: plan.md §3.2 rule STUDENT_ALREADY_ENROLLED — all ExecutionStudent.userAggregateId distinct
        given:
        def execution = anExecution()
        execution.addStudent(aStudent(EXECUTION_STUDENT_USER_AGGREGATE_ID))
        execution.addStudent(aStudent(EXECUTION_STUDENT_USER_AGGREGATE_ID))

        when:
        execution.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.STUDENT_ALREADY_ENROLLED
    }

    def "execution: STUDENT_ALREADY_ENROLLED holds for two distinct students"() {
        // Spec: plan.md §3.2 rule STUDENT_ALREADY_ENROLLED — satisfying representative
        given:
        def execution = anExecution()
        execution.addStudent(aStudent(EXECUTION_STUDENT_USER_AGGREGATE_ID))
        execution.addStudent(aStudent(EXECUTION_STUDENT_USER_AGGREGATE_ID_2))

        when:
        execution.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    // STUDENT_ALREADY_ENROLLED is a categorical uniqueness rule, so testing.md § Choosing Input Values
    // requires no boundary-straddling pair. The Course reference (courseAggregateId, courseName,
    // courseType) is immutable per domain-model §2 and is held in Java `final` fields, which § T1
    // excludes from coverage.

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
