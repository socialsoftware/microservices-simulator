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
class UpdateStudentNameTest extends QuizzesSpockTest {
    private static final String UPDATED_NAME = "UpdatedName"

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

    def "update student name successfully"() {
        given:
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)

        when:
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userDto.getAggregateId(), updateNameDto)

        then:
        def result = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        result.students.find { it.aggregateId == userDto.aggregateId }.name == UPDATED_NAME
    }

    def "update student name with null name throws exception"() {
        given:
        def updateNameDto = new UserDto()
        // name intentionally left null

        when:
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userDto.getAggregateId(), updateNameDto)

        then:
        thrown(QuizzesException)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
