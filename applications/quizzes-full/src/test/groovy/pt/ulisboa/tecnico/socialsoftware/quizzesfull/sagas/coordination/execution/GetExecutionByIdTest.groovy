package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.execution

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetExecutionByIdTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    ExecutionDto executionDto

    def setup() {
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionDto = createExecution(course.aggregateId, "ES001", "1 Semester")
    }

    def "getExecutionById: success"() {
        when:
        def result = executionFunctionalities.getExecutionById(executionDto.aggregateId)

        then:
        result != null
        result.aggregateId == executionDto.aggregateId
        result.acronym == "ES001"
        result.academicTerm == "1 Semester"
    }

    def "getExecutionById: execution not found"() {
        when:
        executionFunctionalities.getExecutionById(999999)

        then:
        thrown(SimulatorException)
    }
}
