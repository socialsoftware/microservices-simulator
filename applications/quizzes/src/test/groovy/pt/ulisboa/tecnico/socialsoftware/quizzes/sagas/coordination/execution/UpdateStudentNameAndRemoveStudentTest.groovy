package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveStudentFromCourseExecutionFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.UpdateStudentNameFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

/**
 * Concurrency tests for the interaction between updateStudentName and removeStudentFromCourseExecution.
 *
 * Saga step inventory:
 *   UpdateStudentNameFunctionalitySagas            : updateStudentNameStep  (single step, no lock)
 *   RemoveStudentFromCourseExecutionFunctionalitySagas : getOldCourseExecutionStep (READ_COURSE, forbids READ_COURSE) → removeStudentStep
 *
 * UpdateStudentName has no forbidden states, so it never raises AGGREGATE_BEING_USED_IN_OTHER_SAGA.
 * The tests verify domain-level outcomes (stale reads/writes) for each step-interleaving order.
 */
@DataJpaTest
class UpdateStudentNameAndRemoveStudentTest extends QuizzesSpockTest {

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService
    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private LocalCommandGateway commandGateway

    private CourseExecutionDto courseExecutionDto
    private UserDto userDto

    def unitOfWork1, updateNameFunctionality
    def unitOfWork2, removeStudentFunctionality

    private static final String NEW_NAME = 'Updated Name'

    def setup() {
        courseExecutionDto = createCourseExecution(
                COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)

        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto.aggregateId)

        def nameDto = new UserDto()
        nameDto.setName(NEW_NAME)

        unitOfWork1 = unitOfWorkService.createUnitOfWork(UpdateStudentNameFunctionalitySagas.class.getSimpleName())
        unitOfWork2 = unitOfWorkService.createUnitOfWork(RemoveStudentFromCourseExecutionFunctionalitySagas.class.getSimpleName())

        updateNameFunctionality = new UpdateStudentNameFunctionalitySagas(
                unitOfWorkService, courseExecutionDto.aggregateId, userDto.aggregateId, nameDto, unitOfWork1, commandGateway)
        removeStudentFunctionality = new RemoveStudentFromCourseExecutionFunctionalitySagas(
                unitOfWorkService, courseExecutionDto.aggregateId, userDto.aggregateId, unitOfWork2, commandGateway)
    }

    def cleanup() {}

    // -------------------------------------------------------------------------
    // Sequential baselines
    // -------------------------------------------------------------------------

    def 'sequential: update name; remove student'() {
        given: 'student name is updated'
        def nameDto = new UserDto()
        nameDto.setName(NEW_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.aggregateId, userDto.aggregateId, nameDto)

        when: 'student is removed'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)

        then: 'execution has no students'
        courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId).students.isEmpty()
    }

    def 'sequential: remove student; update name'() {
        given: 'student is removed'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)

        when: 'try to update removed student'
        def nameDto = new UserDto()
        nameDto.setName(NEW_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.aggregateId, userDto.aggregateId, nameDto)

        then: 'student not found in the execution → exception'
        thrown(Exception)
    }

    // -------------------------------------------------------------------------
    // Concurrent: remove pauses at getOldCourseExecutionStep, update runs first
    // -------------------------------------------------------------------------

    def 'concurrent: remove - getOldCourseExecutionStep; update name; remove - resume'() {
        given: 'remove pauses after acquiring read snapshot'
        removeStudentFunctionality.executeUntilStep('getOldCourseExecutionStep', unitOfWork2)

        when: 'update runs to completion — no saga lock prevents it'
        def nameDto = new UserDto()
        nameDto.setName(NEW_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.aggregateId, userDto.aggregateId, nameDto)

        then: 'name is updated'
        def interim = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        interim.students.find { it.aggregateId == userDto.aggregateId }.name == NEW_NAME

        when: 'remove resumes and completes'
        removeStudentFunctionality.resumeWorkflow(unitOfWork2)

        then: 'student is removed'
        courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId).students.isEmpty()
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def 'concurrent: remove - getOldCourseExecutionStep; update - updateStudentNameStep; remove - resume; update - resume'() {
        given: 'remove pauses after getOldCourseExecutionStep'
        removeStudentFunctionality.executeUntilStep('getOldCourseExecutionStep', unitOfWork2)

        and: 'update pauses at updateStudentNameStep'
        updateNameFunctionality.executeUntilStep('updateStudentNameStep', unitOfWork1)

        when: 'remove resumes first and removes the student'
        removeStudentFunctionality.resumeWorkflow(unitOfWork2)

        then: 'student is removed, saga state cleared'
        courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId).students.isEmpty()
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        when: 'update resumes — it works on its pre-loaded snapshot and commits a new version'
        updateNameFunctionality.resumeWorkflow(unitOfWork1)

        then: 'update applies despite the intermediate removal (saga anomaly: stale read)'
        noExceptionThrown()
    }

    // -------------------------------------------------------------------------
    // Concurrent: update name runs first (single step)
    // -------------------------------------------------------------------------

    def 'concurrent: update - updateStudentNameStep; remove; update - resume'() {
        given: 'update pauses at updateStudentNameStep'
        updateNameFunctionality.executeUntilStep('updateStudentNameStep', unitOfWork1)

        when: 'remove runs to completion — not blocked by update'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)

        then: 'student is removed'
        courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId).students.isEmpty()
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        when: 'update resumes — it had already snapshotted the student before it was removed'
        updateNameFunctionality.resumeWorkflow(unitOfWork1)

        then: 'update applies on the pre-removal snapshot (saga anomaly: stale write)'
        noExceptionThrown()
    }

    def 'concurrent: update - updateStudentNameStep; remove - getOldCourseExecutionStep; update - resume; remove - resume'() {
        given: 'update pauses at updateStudentNameStep'
        updateNameFunctionality.executeUntilStep('updateStudentNameStep', unitOfWork1)

        and: 'remove pauses after getOldCourseExecutionStep (READ_COURSE state set on execution)'
        removeStudentFunctionality.executeUntilStep('getOldCourseExecutionStep', unitOfWork2)

        when: 'update resumes first — no saga lock prevents it'
        updateNameFunctionality.resumeWorkflow(unitOfWork1)

        then: 'student name is updated'
        def interim = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        interim.students.find { it.aggregateId == userDto.aggregateId }.name == NEW_NAME

        when: 'remove resumes'
        removeStudentFunctionality.resumeWorkflow(unitOfWork2)

        then: 'student is removed and saga state is cleared'
        courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId).students.isEmpty()
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
