package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.behaviour.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

@DataJpaTest
class RemoveStudentFaultTest extends QuizzesSpockTest {
    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities

    private CourseExecutionDto courseExecutionDto
    private UserDto userDto

    def setup() {
        loadBehaviorScripts()
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userDto.getAggregateId())
    }

    def cleanup() {
        behaviourService.cleanDirectory()
    }

    def "student is still enrolled when getOldCourseExecutionStep fails"() {
        when:
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        then:
        thrown(SimulatorException)

        and: 'student is still enrolled'
        def result = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        result.students.size() == 1
        result.students.find { it.aggregateId == userDto.aggregateId } != null
    }

    def "student is still enrolled when removeStudentStep fails"() {
        when:
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        then:
        thrown(SimulatorException)

        and: 'student is still enrolled'
        def result = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        result.students.size() == 1
        result.students.find { it.aggregateId == userDto.aggregateId } != null
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
