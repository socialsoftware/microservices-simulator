package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.AddStudentFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveStudentFromCourseExecutionFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

/**
 * Concurrency tests for the interaction between addStudent and removeStudentFromCourseExecution.
 *
 * Saga step inventory:
 *   AddStudentFunctionalitySagas               : getUserStep → enrollStudentStep
 *   RemoveStudentFromCourseExecutionFunctionalitySagas : getOldCourseExecutionStep (READ_COURSE, forbids READ_COURSE) → removeStudentStep
 *
 * AddStudent has no forbidden states, so it never raises AGGREGATE_BEING_USED_IN_OTHER_SAGA
 * against RemoveStudent. The tests verify domain-level outcomes for each step-interleaving order.
 */
@DataJpaTest
class AddStudentAndRemoveStudentTest extends QuizzesSpockTest {

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService
    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private LocalCommandGateway commandGateway

    private CourseExecutionDto courseExecutionDto
    // userDto  - enrolled from setup (removed by removeStudentFunctionality)
    // userDto2 - not yet enrolled  (enrolled by addStudentFunctionality)
    private UserDto userDto, userDto2

    def unitOfWork1, addStudentFunctionality
    def unitOfWork2, removeStudentFunctionality

    def setup() {
        courseExecutionDto = createCourseExecution(
                COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)

        userDto  = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto.aggregateId)

        userDto2 = createUser(USER_NAME_2, USER_USERNAME_2, STUDENT_ROLE)

        unitOfWork1 = unitOfWorkService.createUnitOfWork(AddStudentFunctionalitySagas.class.getSimpleName())
        unitOfWork2 = unitOfWorkService.createUnitOfWork(RemoveStudentFromCourseExecutionFunctionalitySagas.class.getSimpleName())

        addStudentFunctionality = new AddStudentFunctionalitySagas(
                unitOfWorkService, courseExecutionDto.aggregateId, userDto2.aggregateId, unitOfWork1, commandGateway)
        removeStudentFunctionality = new RemoveStudentFromCourseExecutionFunctionalitySagas(
                unitOfWorkService, courseExecutionDto.aggregateId, userDto.aggregateId, unitOfWork2, commandGateway)
    }

    def cleanup() {}

    // -------------------------------------------------------------------------
    // Sequential baselines
    // -------------------------------------------------------------------------

    def 'sequential: add second student; remove first student'() {
        given: 'second student is enrolled'
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto2.aggregateId)

        when: 'first student is removed'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)

        then: 'only the second student remains'
        def result = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        result.students.size() == 1
        result.students.find { it.aggregateId == userDto2.aggregateId } != null
        result.students.find { it.aggregateId == userDto.aggregateId }  == null
    }

    def 'sequential: remove first student; add second student'() {
        given: 'first student is removed'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)

        when: 'second student is enrolled'
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto2.aggregateId)

        then: 'only the second student is in the execution'
        def result = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        result.students.size() == 1
        result.students.find { it.aggregateId == userDto2.aggregateId } != null
        result.students.find { it.aggregateId == userDto.aggregateId }  == null
    }

    // -------------------------------------------------------------------------
    // Concurrent: add pauses at getUserStep.
    // getUserStep resolves user data without touching the execution aggregate,
    // so the execution is free for remove to run concurrently.
    // -------------------------------------------------------------------------

    def 'concurrent: add - getUserStep; remove; add - resume'() {
        given: 'add pauses after getUserStep — execution not yet involved'
        addStudentFunctionality.executeUntilStep('getUserStep', unitOfWork1)

        when: 'remove completes while add is paused'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)

        then: 'remove succeeds'
        def interim = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        interim.students.size() == 0
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        when: 'add resumes — execution is free after remove committed'
        addStudentFunctionality.resumeWorkflow(unitOfWork1)

        then: 'second student is now enrolled'
        def result = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        result.students.size() == 1
        result.students.find { it.aggregateId == userDto2.aggregateId } != null
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def 'concurrent: add - getUserStep; remove - getOldCourseExecutionStep; add - resume; remove - resume'() {
        given: 'add pauses after getUserStep'
        addStudentFunctionality.executeUntilStep('getUserStep', unitOfWork1)

        and: 'remove pauses after getOldCourseExecutionStep (READ_COURSE state set)'
        removeStudentFunctionality.executeUntilStep('getOldCourseExecutionStep', unitOfWork2)

        when: 'add resumes (enrollStudentStep) — no saga locking prevents it'
        addStudentFunctionality.resumeWorkflow(unitOfWork1)

        then: 'second student is enrolled successfully'
        def interim = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        interim.students.find { it.aggregateId == userDto2.aggregateId } != null

        when: 'remove resumes and completes'
        removeStudentFunctionality.resumeWorkflow(unitOfWork2)

        then: 'first student is removed'
        def result = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        result.students.size() == 1
        result.students.find { it.aggregateId == userDto.aggregateId }  == null
        result.students.find { it.aggregateId == userDto2.aggregateId } != null
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    // -------------------------------------------------------------------------
    // Concurrent: remove pauses at getOldCourseExecutionStep, add runs concurrently
    // -------------------------------------------------------------------------

    def 'concurrent: remove - getOldCourseExecutionStep; add; remove - resume'() {
        given: 'remove pauses after getOldCourseExecutionStep (READ_COURSE state set)'
        removeStudentFunctionality.executeUntilStep('getOldCourseExecutionStep', unitOfWork2)

        when: 'add runs to completion — no lock blocks it'
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto2.aggregateId)

        then: 'second student is enrolled'
        def interim = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        interim.students.size() == 2

        when: 'remove resumes and removes the first student'
        removeStudentFunctionality.resumeWorkflow(unitOfWork2)

        then: 'only the second student remains'
        def result = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        result.students.size() == 1
        result.students.find { it.aggregateId == userDto2.aggregateId } != null
        result.students.find { it.aggregateId == userDto.aggregateId }  == null
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def 'concurrent: remove - getOldCourseExecutionStep; add - getUserStep; remove - resume; add - resume'() {
        given: 'remove pauses after getOldCourseExecutionStep'
        removeStudentFunctionality.executeUntilStep('getOldCourseExecutionStep', unitOfWork2)

        and: 'add pauses after getUserStep'
        addStudentFunctionality.executeUntilStep('getUserStep', unitOfWork1)

        when: 'remove resumes and completes'
        removeStudentFunctionality.resumeWorkflow(unitOfWork2)

        then: 'first student removed'
        def result = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        result.students.size() == 0
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        when: 'add resumes — execution is free'
        addStudentFunctionality.resumeWorkflow(unitOfWork1)

        then: 'second student enrolled'
        def result2 = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        result2.students.size() == 1
        result2.students.find { it.aggregateId == userDto2.aggregateId } != null
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
