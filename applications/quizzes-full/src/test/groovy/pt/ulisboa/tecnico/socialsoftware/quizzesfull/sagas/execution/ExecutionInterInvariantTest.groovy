package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.notification.handling.ExecutionEventHandling

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class ExecutionInterInvariantTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    ExecutionEventHandling executionEventHandling

    // ─── USER_EXISTS — DeleteUserEvent ───────────────────────────────────────

    def "execution removes student when user is deleted"() {
        given:
        def user = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def execution = createExecution(course.aggregateId, "EA001", "1st Semester 2024/25")
        executionFunctionalities.enrollStudentInExecution(execution.aggregateId, user.aggregateId)

        when: 'user is deleted, publishing DeleteUserEvent'
        userFunctionalities.deleteUser(user.aggregateId)

        and: 'execution polls for delete user events'
        executionEventHandling.handleDeleteUserEvents()

        then: 'student is removed from execution'
        executionFunctionalities.getStudentByExecutionIdAndUserId(execution.aggregateId, user.aggregateId) == null
    }

    def "execution ignores DeleteUserEvent for unrelated user"() {
        given:
        def user1 = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        def user2 = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def execution = createExecution(course.aggregateId, "EA001", "1st Semester 2024/25")
        executionFunctionalities.enrollStudentInExecution(execution.aggregateId, user1.aggregateId)

        when: 'an unrelated user is deleted'
        userFunctionalities.deleteUser(user2.aggregateId)

        and: 'execution polls for delete user events'
        executionEventHandling.handleDeleteUserEvents()

        then: 'user1 is still enrolled in execution'
        executionFunctionalities.getStudentByExecutionIdAndUserId(execution.aggregateId, user1.aggregateId) != null
    }

    // ─── USER_EXISTS — UpdateStudentNameEvent ─────────────────────────────────

    def "execution updates cached student name on UpdateStudentNameEvent"() {
        given:
        def user = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def execution = createExecution(course.aggregateId, "EA001", "1st Semester 2024/25")
        executionFunctionalities.enrollStudentInExecution(execution.aggregateId, user.aggregateId)

        when: 'user name is updated directly, publishing UpdateStudentNameEvent'
        def uow = unitOfWorkService.createUnitOfWork("updateUserName")
        userService.updateUserName(user.aggregateId, USER_NAME_2, uow)
        unitOfWorkService.commit(uow)

        and: 'execution polls for update student name events'
        executionEventHandling.handleUpdateStudentNameEvents()

        then: 'cached student name in execution is updated'
        def student = executionFunctionalities.getStudentByExecutionIdAndUserId(execution.aggregateId, user.aggregateId)
        student.userName == USER_NAME_2
    }

    def "execution ignores UpdateStudentNameEvent for unrelated user"() {
        given:
        def user1 = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        def user2 = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def execution = createExecution(course.aggregateId, "EA001", "1st Semester 2024/25")
        executionFunctionalities.enrollStudentInExecution(execution.aggregateId, user1.aggregateId)

        when: 'an unrelated user name is updated'
        def uow = unitOfWorkService.createUnitOfWork("updateUserName")
        userService.updateUserName(user2.aggregateId, "New Name", uow)
        unitOfWorkService.commit(uow)

        and: 'execution polls for update student name events'
        executionEventHandling.handleUpdateStudentNameEvents()

        then: 'user1 cached name in execution is unchanged'
        def student = executionFunctionalities.getStudentByExecutionIdAndUserId(execution.aggregateId, user1.aggregateId)
        student.userName == USER_NAME_1
    }

    // ─── USER_EXISTS — AnonymizeStudentEvent ──────────────────────────────────

    def "execution anonymizes cached student data on AnonymizeStudentEvent"() {
        given:
        def user = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def execution = createExecution(course.aggregateId, "EA001", "1st Semester 2024/25")
        executionFunctionalities.enrollStudentInExecution(execution.aggregateId, user.aggregateId)

        when: 'user is anonymized directly, publishing AnonymizeStudentEvent'
        def uow = unitOfWorkService.createUnitOfWork("anonymizeUser")
        userService.anonymizeUser(user.aggregateId, uow)
        unitOfWorkService.commit(uow)

        and: 'execution polls for anonymize student events'
        executionEventHandling.handleAnonymizeStudentEvents()

        then: 'cached student data in execution is anonymized'
        def student = executionFunctionalities.getStudentByExecutionIdAndUserId(execution.aggregateId, user.aggregateId)
        student.userName == "ANONYMOUS"
        student.userUsername == "ANONYMOUS"
    }

    def "execution ignores AnonymizeStudentEvent for unrelated user"() {
        given:
        def user1 = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        def user2 = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def execution = createExecution(course.aggregateId, "EA001", "1st Semester 2024/25")
        executionFunctionalities.enrollStudentInExecution(execution.aggregateId, user1.aggregateId)

        when: 'an unrelated user is anonymized'
        def uow = unitOfWorkService.createUnitOfWork("anonymizeUser")
        userService.anonymizeUser(user2.aggregateId, uow)
        unitOfWorkService.commit(uow)

        and: 'execution polls for anonymize student events'
        executionEventHandling.handleAnonymizeStudentEvents()

        then: "user1's cached data in execution is unchanged"
        def student = executionFunctionalities.getStudentByExecutionIdAndUserId(execution.aggregateId, user1.aggregateId)
        student.userName == USER_NAME_1
        student.userUsername == USER_USERNAME_1
    }
}
