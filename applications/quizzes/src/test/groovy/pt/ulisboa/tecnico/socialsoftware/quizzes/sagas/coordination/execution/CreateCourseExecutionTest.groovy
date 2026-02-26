package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.SagaExecution
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@DataJpaTest
class CreateCourseExecutionTest extends QuizzesSpockTest {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService

    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities

    private CourseExecutionDto courseExecutionDto

    def setup() {
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)
    }

    def cleanup() {}

    def "create course execution successfully"() {
        when:
        def result = courseExecutionFunctionalities.createCourseExecution(new CourseExecutionDto(
            name: COURSE_EXECUTION_NAME,
            type: COURSE_EXECUTION_TYPE,
            acronym: 'NEW_' + COURSE_EXECUTION_ACRONYM,
            academicTerm: COURSE_EXECUTION_ACADEMIC_TERM,
            endDate: DateHandler.toISOString(TIME_4)
        ))

        then:
        result != null
        result.name == COURSE_EXECUTION_NAME
        result.type == COURSE_EXECUTION_TYPE
        result.acronym == 'NEW_' + COURSE_EXECUTION_ACRONYM
        result.academicTerm == COURSE_EXECUTION_ACADEMIC_TERM
        LocalDateTime.parse(result.endDate, DateTimeFormatter.ISO_DATE_TIME) == TIME_4

        def unitOfWork = unitOfWorkService.createUnitOfWork("TEST")
        def courseExecution = (SagaExecution) unitOfWorkService.aggregateLoadAndRegisterRead(result.getAggregateId(), unitOfWork)
        courseExecution.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    def "create course execution with invalid input"() {
        when:
        courseExecutionFunctionalities.createCourseExecution(new CourseExecutionDto(
            name: COURSE_EXECUTION_NAME,
            type: COURSE_EXECUTION_TYPE,
            acronym: null,
            academicTerm: COURSE_EXECUTION_ACADEMIC_TERM,
            endDate: DateHandler.toISOString(TIME_4)
        ))

        then:
        thrown(QuizzesException)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
