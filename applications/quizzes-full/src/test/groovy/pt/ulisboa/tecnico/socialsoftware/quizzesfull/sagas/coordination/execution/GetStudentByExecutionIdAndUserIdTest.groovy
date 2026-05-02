package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.execution

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetStudentByExecutionIdAndUserIdTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    ExecutionDto executionDto
    UserDto userDto

    def setup() {
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionDto = createExecution(course.aggregateId, "ES001", "1 Semester")
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
    }

    def "getStudentByExecutionIdAndUserId: success"() {
        given:
        executionFunctionalities.enrollStudentInExecution(executionDto.aggregateId, userDto.aggregateId)

        when:
        def result = executionFunctionalities.getStudentByExecutionIdAndUserId(executionDto.aggregateId, userDto.aggregateId)

        then:
        result != null
        result.userAggregateId == userDto.aggregateId
        result.userName == USER_NAME_1
        result.userUsername == USER_USERNAME_1
    }

    def "getStudentByExecutionIdAndUserId: execution not found"() {
        when:
        executionFunctionalities.getStudentByExecutionIdAndUserId(999999, userDto.aggregateId)

        then:
        thrown(SimulatorException)
    }

    def "getStudentByExecutionIdAndUserId: student not enrolled returns null"() {
        when:
        def result = executionFunctionalities.getStudentByExecutionIdAndUserId(executionDto.aggregateId, userDto.aggregateId)

        then:
        result == null
    }
}
