package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.execution

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto

@DataJpaTest
@Transactional
@Import(CreateExecutionTest.LocalBeanConfiguration)
class CreateExecutionTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_COURSE_AGGREGATE_ID = 999999

    def "createExecution: success"() {
        // Spec: plan.md §4 Execution — CreateExecution(courseAggregateId, acronym, academicTerm, endDate)
        given: 'an existing course'
        def courseAggregateId = createCourse()
        def executionDto = new ExecutionDto()
        executionDto.setCourseAggregateId(courseAggregateId)
        executionDto.setAcronym(EXECUTION_ACRONYM)
        executionDto.setAcademicTerm(EXECUTION_ACADEMIC_TERM)
        executionDto.setEndDate(EXECUTION_END_DATE)

        when:
        def result = executionFunctionalities.createExecution(executionDto)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.aggregateId != null
        result.acronym == EXECUTION_ACRONYM
        result.academicTerm == EXECUTION_ACADEMIC_TERM
        result.endDate == EXECUTION_END_DATE
        result.courseAggregateId == courseAggregateId
        result.courseName == COURSE_NAME
        result.courseType == COURSE_TYPE
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createExecution: aborts when the course prerequisite does not exist"() {
        // Spec: plan.md §4 Execution — cross-aggregate prerequisite (P4a): getCourseStep fetches the
        // course and throws if it does not exist, so no explicit guard is written
        given:
        def executionDto = new ExecutionDto()
        executionDto.setCourseAggregateId(NONEXISTENT_COURSE_AGGREGATE_ID)
        executionDto.setAcronym(EXECUTION_ACRONYM)
        executionDto.setAcademicTerm(EXECUTION_ACADEMIC_TERM)
        executionDto.setEndDate(EXECUTION_END_DATE)

        when:
        executionFunctionalities.createExecution(executionDto)

        then:
        thrown(SimulatorException)
    }

    // Semantic-lock acquisition: CreateExecutionFunctionalitySagas has no setSemanticLock step — the
    // create step brings the aggregate into existence (sagas.md § Create Functionality Sagas), so
    // there is no prior state to lock, and getCourseStep is a plain upstream read. No compensation
    // test either: the create is the saga's last step, so no lock is held across a later step
    // (testing.md § Compensation Test, applicability test).

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
