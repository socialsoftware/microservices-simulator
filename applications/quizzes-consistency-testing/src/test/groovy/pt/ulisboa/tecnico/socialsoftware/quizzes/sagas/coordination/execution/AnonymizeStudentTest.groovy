package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

@DataJpaTest
class AnonymizeStudentTest extends QuizzesSpockTest {
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

    def "anonymize student successfully"() {
        when:
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        then:
        def result = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        result.students.find { it.aggregateId == userDto.aggregateId }.name == ANONYMOUS
        result.students.find { it.aggregateId == userDto.aggregateId }.username == ANONYMOUS
    }

    def "anonymize student not enrolled throws exception"() {
        given: 'a student not enrolled in the course execution'
        def userDto2 = createUser(USER_NAME_2, USER_USERNAME_2, STUDENT_ROLE)

        when:
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.getAggregateId(), userDto2.getAggregateId())

        then:
        thrown(QuizzesException)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
