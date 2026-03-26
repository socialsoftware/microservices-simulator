package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

@DataJpaTest
class ExecutionReadOperationsTest extends QuizzesSpockTest {
    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities

    private CourseExecutionDto courseExecutionDto
    private UserDto userDto

    def setup() {
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userDto.getAggregateId())
    }

    def cleanup() {}

    // ----- getCourseExecutionById -----

    def "get course execution by id returns the correct execution"() {
        when:
        def result = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())

        then:
        result != null
        result.aggregateId == courseExecutionDto.aggregateId
        result.acronym == COURSE_EXECUTION_ACRONYM
        result.name == COURSE_EXECUTION_NAME
    }

    def "get course execution by non-existent id throws exception"() {
        when:
        courseExecutionFunctionalities.getCourseExecutionByAggregateId(99999)

        then:
        thrown(Exception)
    }

    // ----- getAllCourseExecutions -----

    def "get all course executions returns all created executions"() {
        when:
        def result = courseExecutionFunctionalities.getCourseExecutions()

        then:
        result != null
        result.size() >= 1
        result.find { it.aggregateId == courseExecutionDto.aggregateId } != null
    }

    def "get all course executions returns multiple executions after creating more"() {
        given: 'a second course execution'
        def courseExecutionDto2 = createCourseExecution(COURSE_EXECUTION_NAME + '_2', COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM + '_2', COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)

        when:
        def result = courseExecutionFunctionalities.getCourseExecutions()

        then:
        result.size() >= 2
        result.find { it.aggregateId == courseExecutionDto.aggregateId } != null
        result.find { it.aggregateId == courseExecutionDto2.aggregateId } != null
    }

    // ----- getCourseExecutionsByUserId -----

    def "get course executions by user id returns executions the user is enrolled in"() {
        when:
        def result = courseExecutionFunctionalities.getCourseExecutionsByUser(userDto.getAggregateId())

        then:
        result != null
        result.find { it.aggregateId == courseExecutionDto.aggregateId } != null
    }

    def "get course executions by user id returns empty for user not enrolled in any execution"() {
        given: 'a user not enrolled in any execution'
        def userDto2 = createUser(USER_NAME_2, USER_USERNAME_2, STUDENT_ROLE)

        when:
        def result = courseExecutionFunctionalities.getCourseExecutionsByUser(userDto2.getAggregateId())

        then:
        result != null
        result.isEmpty()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
