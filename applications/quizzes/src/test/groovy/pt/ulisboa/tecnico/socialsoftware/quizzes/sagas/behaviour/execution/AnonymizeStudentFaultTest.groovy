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
class AnonymizeStudentFaultTest extends QuizzesSpockTest {
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

    def "student is not anonymized when getCourseExecutionStep fails"() {
        when:
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        then:
        thrown(SimulatorException)

        and: 'the student is not anonymized'
        def result = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        result.students.find { it.aggregateId == userDto.aggregateId }.name == USER_NAME_1
        result.students.find { it.aggregateId == userDto.aggregateId }.username == USER_USERNAME_1
    }

    def "student is not anonymized when anonymizeStudentStep fails"() {
        when:
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        then:
        thrown(SimulatorException)

        and: 'the student is not anonymized — the saga aborted before the write was committed'
        def result = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        result.students.find { it.aggregateId == userDto.aggregateId }.name == USER_NAME_1
        result.students.find { it.aggregateId == userDto.aggregateId }.username == USER_USERNAME_1
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
