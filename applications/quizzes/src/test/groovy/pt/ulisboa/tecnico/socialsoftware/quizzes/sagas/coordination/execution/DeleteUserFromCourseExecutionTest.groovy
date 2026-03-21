package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.handling.CourseExecutionEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities

@DataJpaTest
class DeleteUserFromCourseExecutionTest extends QuizzesSpockTest {

    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private UserFunctionalities userFunctionalities
    @Autowired
    private CourseExecutionEventHandling courseExecutionEventHandling

    private CourseExecutionDto courseExecutionDto
    private UserDto userDto

    def setup() {
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userDto.getAggregateId())
    }

    def cleanup() {}

    def 'enroll student; deactivate and delete user; before event processing student is still in execution'() {
        when: 'user is deactivated and deleted'
        userFunctionalities.deactivateUser(userDto.getAggregateId())
        userFunctionalities.deleteUser(userDto.getAggregateId())

        then: 'student is still enrolled before the event is processed'
        def result = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        result.students.size() == 1
    }

    /*
     * This test covers the event-driven removal path: DeleteUserEvent → handleRemoveUserEvents()
     * → removeUser() → student.setActive(false).
     *
     * If allStudentsAreActive() were uncommented in verifyInvariants(), this test would fail
     * with INVARIANT_BREAK because removeUser() calls registerChanged() while the student
     * is still in the set but inactive — a transient state that is invalid under that invariant.
     */
    def 'enroll student; deactivate and delete user; process event - student is set inactive in execution'() {
        given: 'user is deactivated and deleted'
        userFunctionalities.deactivateUser(userDto.getAggregateId())
        userFunctionalities.deleteUser(userDto.getAggregateId())

        when: 'delete user event is processed'
        courseExecutionEventHandling.handleRemoveUserEvents()

        then: 'student is inactive in execution after event processing'
        def result = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        result.students.size() == 1
        !result.students.find { it.aggregateId == userDto.aggregateId }.active
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
