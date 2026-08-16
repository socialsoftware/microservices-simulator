package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.domain.QuizzesFull2DomainConstants
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionStudentDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.notification.handling.ExecutionEventHandling

@DataJpaTest
@Transactional
@Import(ExecutionInterInvariantTest.LocalBeanConfiguration)
class ExecutionInterInvariantTest extends QuizzesFull2SpockTest {

    public static final String STUDENT_A_NAME = "Bob Jones"
    public static final String STUDENT_A_USERNAME = "bob"
    public static final String STUDENT_B_NAME = "Carol White"
    public static final String STUDENT_B_USERNAME = "carol"
    public static final String UPDATED_STUDENT_NAME = "Bob J. Jones"

    @Autowired
    ExecutionEventHandling executionEventHandling

    def "execution refreshes the cached student snapshot on ActivateUserEvent"() {
        // Spec: plan.md §4 Execution — subscribed ActivateUserEvent; grouping §2 Execution/User row
        // caches (active, userVersion). Enrollment already requires an active user (INACTIVE_USER),
        // so `active` can only be re-affirmed true; the cached publisher version is what moves.
        given:
        def executionAggregateId = createExecution(createCourse())
        def studentAggregateId = createActiveUser(STUDENT_A_NAME, STUDENT_A_USERNAME)
        enrollStudentInExecution(executionAggregateId, studentAggregateId)
        drainActivationBacklog()
        def versionBefore = studentOf(executionAggregateId, studentAggregateId).userVersion

        when: 'the user is activated'
        userFunctionalities.activateUser(studentAggregateId)

        and: 'the execution polls for the event'
        executionEventHandling.handleActivateUserEvents()

        then: 'the cached student snapshot carries the payload and advances past the published version'
        def student = studentOf(executionAggregateId, studentAggregateId)
        student.isActive()
        student.userVersion > versionBefore
    }

    def "execution ignores an ActivateUserEvent for a user it does not cache"() {
        // Spec: plan.md §4 Execution — the subscription is anchored on the enrolled student's user id
        given:
        def executionAggregateId = createExecution(createCourse())
        def studentAggregateId = createActiveUser(STUDENT_A_NAME, STUDENT_A_USERNAME)
        enrollStudentInExecution(executionAggregateId, studentAggregateId)
        def unrelatedUserAggregateId = createActiveUser(STUDENT_B_NAME, STUDENT_B_USERNAME)
        drainActivationBacklog()
        def versionBefore = studentOf(executionAggregateId, studentAggregateId).userVersion

        when: 'an unenrolled user is activated'
        userFunctionalities.activateUser(unrelatedUserAggregateId)

        and: 'the execution polls for the event'
        executionEventHandling.handleActivateUserEvents()

        then: 'the enrolled student snapshot is untouched'
        studentOf(executionAggregateId, studentAggregateId).userVersion == versionBefore
    }

    def "execution updates the cached student name on UpdateStudentNameEvent"() {
        // Spec: plan.md §4 Execution — subscribed UpdateStudentNameEvent; payload field updatedName
        given:
        def executionAggregateId = createExecution(createCourse())
        def studentAggregateId = createActiveUser(STUDENT_A_NAME, STUDENT_A_USERNAME)
        enrollStudentInExecution(executionAggregateId, studentAggregateId)

        when: 'the user name is updated'
        userFunctionalities.updateUserName(studentAggregateId, UPDATED_STUDENT_NAME)

        and: 'the execution polls for the event'
        executionEventHandling.handleUpdateStudentNameEvents()

        then: 'the cached student name is the updated one'
        def student = studentOf(executionAggregateId, studentAggregateId)
        student.userName == UPDATED_STUDENT_NAME
        student.userUsername == STUDENT_A_USERNAME
    }

    def "execution ignores an UpdateStudentNameEvent for another enrolled student"() {
        // Spec: plan.md §4 Execution — each cached student has its own anchor
        given:
        def executionAggregateId = createExecution(createCourse())
        def studentAggregateId = createActiveUser(STUDENT_A_NAME, STUDENT_A_USERNAME)
        def otherStudentAggregateId = createActiveUser(STUDENT_B_NAME, STUDENT_B_USERNAME)
        enrollStudentInExecution(executionAggregateId, studentAggregateId)
        enrollStudentInExecution(executionAggregateId, otherStudentAggregateId)
        def nameBefore = studentOf(executionAggregateId, studentAggregateId).userName

        when: 'the other student is renamed'
        userFunctionalities.updateUserName(otherStudentAggregateId, UPDATED_STUDENT_NAME)

        and: 'the execution polls for the event'
        executionEventHandling.handleUpdateStudentNameEvents()

        then: 'only the renamed student is affected'
        studentOf(executionAggregateId, studentAggregateId).userName == nameBefore
        studentOf(executionAggregateId, otherStudentAggregateId).userName == UPDATED_STUDENT_NAME
    }

    def "execution anonymizes the cached student snapshot on AnonymizeStudentEvent"() {
        // Spec: plan.md §4 Execution — subscribed AnonymizeStudentEvent; payload fields name, username
        given:
        def executionAggregateId = createExecution(createCourse())
        def studentAggregateId = createActiveUser(STUDENT_A_NAME, STUDENT_A_USERNAME)
        enrollStudentInExecution(executionAggregateId, studentAggregateId)

        when: 'the user is anonymized'
        userFunctionalities.anonymizeUser(studentAggregateId)

        and: 'the execution polls for the event'
        executionEventHandling.handleAnonymizeStudentEvents()

        then: 'both cached identity fields are anonymized'
        def student = studentOf(executionAggregateId, studentAggregateId)
        student.userName == QuizzesFull2DomainConstants.ANONYMOUS
        student.userUsername == QuizzesFull2DomainConstants.ANONYMOUS
    }

    def "execution ignores an AnonymizeStudentEvent for another enrolled student"() {
        // Spec: plan.md §4 Execution — each cached student has its own anchor
        given:
        def executionAggregateId = createExecution(createCourse())
        def studentAggregateId = createActiveUser(STUDENT_A_NAME, STUDENT_A_USERNAME)
        def otherStudentAggregateId = createActiveUser(STUDENT_B_NAME, STUDENT_B_USERNAME)
        enrollStudentInExecution(executionAggregateId, studentAggregateId)
        enrollStudentInExecution(executionAggregateId, otherStudentAggregateId)
        def nameBefore = studentOf(executionAggregateId, studentAggregateId).userName
        def usernameBefore = studentOf(executionAggregateId, studentAggregateId).userUsername

        when: 'the other student is anonymized'
        userFunctionalities.anonymizeUser(otherStudentAggregateId)

        and: 'the execution polls for the event'
        executionEventHandling.handleAnonymizeStudentEvents()

        then: 'only the anonymized student is affected'
        def student = studentOf(executionAggregateId, studentAggregateId)
        student.userName == nameBefore
        student.userUsername == usernameBefore
        studentOf(executionAggregateId, otherStudentAggregateId).userName == QuizzesFull2DomainConstants.ANONYMOUS
    }

    def "execution drops the cached student on DeleteUserEvent"() {
        // Spec: plan.md §3.2 — rule USER_EXISTS (Execution), P2: no enrolled student may reference a
        // deleted User. The execution stays valid without that student, so only the member is removed.
        given:
        def executionAggregateId = createExecution(createCourse())
        def studentAggregateId = createActiveUser(STUDENT_A_NAME, STUDENT_A_USERNAME)
        enrollStudentInExecution(executionAggregateId, studentAggregateId)

        when: 'the user is deleted'
        userFunctionalities.deleteUser(studentAggregateId)

        and: 'the execution polls for the event'
        executionEventHandling.handleDeleteUserEvents()

        then: 'the student is gone from the roster while the execution itself survives'
        studentOf(executionAggregateId, studentAggregateId) == null
        executionService.getExecutionById(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check")).students.isEmpty()
    }

    def "execution ignores a DeleteUserEvent for another enrolled student"() {
        // Spec: plan.md §3.2 — rule USER_EXISTS (Execution) removes only the deleted student
        given:
        def executionAggregateId = createExecution(createCourse())
        def studentAggregateId = createActiveUser(STUDENT_A_NAME, STUDENT_A_USERNAME)
        def otherStudentAggregateId = createActiveUser(STUDENT_B_NAME, STUDENT_B_USERNAME)
        enrollStudentInExecution(executionAggregateId, studentAggregateId)
        enrollStudentInExecution(executionAggregateId, otherStudentAggregateId)
        def nameBefore = studentOf(executionAggregateId, studentAggregateId).userName

        when: 'the other student user is deleted'
        userFunctionalities.deleteUser(otherStudentAggregateId)

        and: 'the execution polls for the event'
        executionEventHandling.handleDeleteUserEvents()

        then: 'the surviving student keeps its cached snapshot'
        def student = studentOf(executionAggregateId, studentAggregateId)
        student != null
        student.userName == nameBefore

        and: 'only the deleted student left the roster'
        studentOf(executionAggregateId, otherStudentAggregateId) == null
    }

    // A student fixture is a created user plus an activation, and that activation's event outranks the
    // snapshot version the later enrollment seeds - so it is still pending when the test starts. Drain
    // it first, or the assertion cannot tell the fixture's own event from the one the test fires.
    private void drainActivationBacklog() {
        executionEventHandling.handleActivateUserEvents()
    }

    private ExecutionStudentDto studentOf(Integer executionAggregateId, Integer userAggregateId) {
        return executionService.getExecutionById(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
                .students.find { it.userAggregateId == userAggregateId }
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
