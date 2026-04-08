package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.AnonymizeStudentFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveStudentFromCourseExecutionFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

/**
 * Concurrency tests for the interaction between anonymizeStudent and removeStudentFromCourseExecution.
 *
 * Saga step inventory:
 *   AnonymizeStudentFunctionalitySagas            : getCourseExecutionStep (READ_COURSE, forbids READ_COURSE) → anonymizeStudentStep
 *   RemoveStudentFromCourseExecutionFunctionalitySagas : getOldCourseExecutionStep (READ_COURSE, forbids READ_COURSE) → removeStudentStep
 *
 * Both sagas set and forbid READ_COURSE, making them mutually exclusive while either holds the lock.
 */
@DataJpaTest
class AnonymizeStudentAndRemoveStudentTest extends QuizzesSpockTest {

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService
    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private LocalCommandGateway commandGateway

    private CourseExecutionDto courseExecutionDto
    private UserDto userDto

    def unitOfWork1, anonymizeFunctionality
    def unitOfWork2, removeStudentFunctionality

    def setup() {
        courseExecutionDto = createCourseExecution(
                COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)

        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto.aggregateId)

        unitOfWork1 = unitOfWorkService.createUnitOfWork(AnonymizeStudentFunctionalitySagas.class.getSimpleName())
        unitOfWork2 = unitOfWorkService.createUnitOfWork(RemoveStudentFromCourseExecutionFunctionalitySagas.class.getSimpleName())

        anonymizeFunctionality = new AnonymizeStudentFunctionalitySagas(
                unitOfWorkService, courseExecutionDto.aggregateId, userDto.aggregateId, unitOfWork1, commandGateway)
        removeStudentFunctionality = new RemoveStudentFromCourseExecutionFunctionalitySagas(
                unitOfWorkService, courseExecutionDto.aggregateId, userDto.aggregateId, unitOfWork2, commandGateway)
    }

    def cleanup() {}

    // -------------------------------------------------------------------------
    // Sequential baselines
    // -------------------------------------------------------------------------

    def 'sequential: anonymize; remove'() {
        given: 'student is anonymized'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userDto.aggregateId)

        when: 'student is removed from execution'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)

        then: 'execution has no students'
        courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId).students.isEmpty()
    }

    def 'sequential: remove; anonymize'() {
        given: 'student is removed from execution'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)

        when: 'try to anonymize the removed student'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userDto.aggregateId)

        then: 'student not in execution → exception'
        thrown(QuizzesException)
    }

    // -------------------------------------------------------------------------
    // Concurrent: anonymize acquires READ_COURSE first; remove blocked
    // -------------------------------------------------------------------------

    def 'concurrent: anonymize - getCourseExecutionStep; remove; anonymize - resume'() {
        given: 'anonymize pauses after getCourseExecutionStep (READ_COURSE held)'
        anonymizeFunctionality.executeUntilStep('getCourseExecutionStep', unitOfWork1)

        when: 'remove tries to run fully — blocked by READ_COURSE'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)
        then: 'aggregate is being used in another saga'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'anonymize resumes and commits'
        anonymizeFunctionality.resumeWorkflow(unitOfWork1)
        then: 'student is anonymized; lock released'
        def interim = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        interim.students.find { it.aggregateId == userDto.aggregateId }.name == ANONYMOUS
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        when: 'remove is retried'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)
        then: 'execution has no students'
        courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId).students.isEmpty()
    }

    def 'concurrent: anonymize - getCourseExecutionStep; remove - getOldCourseExecutionStep; anonymize - resume; remove retry'() {
        given: 'anonymize pauses after getCourseExecutionStep (READ_COURSE held)'
        anonymizeFunctionality.executeUntilStep('getCourseExecutionStep', unitOfWork1)

        when: 'remove tries to pause at getOldCourseExecutionStep — blocked by READ_COURSE'
        removeStudentFunctionality.executeUntilStep('getOldCourseExecutionStep', unitOfWork2)
        then: 'aggregate is being used in another saga'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'anonymize resumes and commits'
        anonymizeFunctionality.resumeWorkflow(unitOfWork1)
        then: 'student is anonymized; lock released'
        def interim = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        interim.students.find { it.aggregateId == userDto.aggregateId }.name == ANONYMOUS
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        when: 'remove is retried'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)
        then: 'execution has no students'
        courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId).students.isEmpty()
    }

    def 'concurrent: anonymize - anonymizeStudentStep; remove; anonymize - resume'() {
        given: 'anonymize pauses at anonymizeStudentStep (READ_COURSE still held)'
        anonymizeFunctionality.executeUntilStep('anonymizeStudentStep', unitOfWork1)

        when: 'remove tries to run fully — still blocked by READ_COURSE'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)
        then: 'aggregate is being used in another saga'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'anonymize resumes and commits'
        anonymizeFunctionality.resumeWorkflow(unitOfWork1)
        then: 'student is anonymized; lock released'
        def interim = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        interim.students.find { it.aggregateId == userDto.aggregateId }.name == ANONYMOUS
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        when: 'remove is retried'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)
        then: 'execution has no students'
        courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId).students.isEmpty()
    }

    def 'concurrent: anonymize - anonymizeStudentStep; remove - getOldCourseExecutionStep; anonymize - resume; remove retry'() {
        given: 'anonymize pauses at anonymizeStudentStep (READ_COURSE still held)'
        anonymizeFunctionality.executeUntilStep('anonymizeStudentStep', unitOfWork1)

        when: 'remove tries to pause at getOldCourseExecutionStep — still blocked'
        removeStudentFunctionality.executeUntilStep('getOldCourseExecutionStep', unitOfWork2)
        then: 'aggregate is being used in another saga'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'anonymize resumes and commits'
        anonymizeFunctionality.resumeWorkflow(unitOfWork1)
        then: 'student is anonymized; lock released'
        def interim = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId)
        interim.students.find { it.aggregateId == userDto.aggregateId }.name == ANONYMOUS
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        when: 'remove is retried'
        courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)
        then: 'execution has no students'
        courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId).students.isEmpty()
    }

    // -------------------------------------------------------------------------
    // Concurrent: remove acquires READ_COURSE first; anonymize blocked
    // -------------------------------------------------------------------------

    def 'concurrent: remove - getOldCourseExecutionStep; anonymize; remove - resume'() {
        given: 'remove pauses after getOldCourseExecutionStep (READ_COURSE held)'
        removeStudentFunctionality.executeUntilStep('getOldCourseExecutionStep', unitOfWork2)

        when: 'anonymize tries to run fully — blocked by READ_COURSE'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userDto.aggregateId)
        then: 'aggregate is being used in another saga'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'remove resumes and commits'
        removeStudentFunctionality.resumeWorkflow(unitOfWork2)
        then: 'student removed; lock released'
        courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId).students.isEmpty()
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        when: 'anonymize is retried — student no longer enrolled'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userDto.aggregateId)
        then: 'student not found in execution → exception'
        thrown(QuizzesException)
    }

    def 'concurrent: remove - getOldCourseExecutionStep; anonymize - getCourseExecutionStep; remove - resume; anonymize retry'() {
        given: 'remove pauses after getOldCourseExecutionStep (READ_COURSE held)'
        removeStudentFunctionality.executeUntilStep('getOldCourseExecutionStep', unitOfWork2)

        when: 'anonymize tries to pause at getCourseExecutionStep — blocked by READ_COURSE'
        anonymizeFunctionality.executeUntilStep('getCourseExecutionStep', unitOfWork1)
        then: 'aggregate is being used in another saga'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'remove resumes and commits'
        removeStudentFunctionality.resumeWorkflow(unitOfWork2)
        then: 'student removed; lock released'
        courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId).students.isEmpty()
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        when: 'anonymize is retried — student no longer enrolled'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userDto.aggregateId)
        then: 'student not found in execution → exception'
        thrown(QuizzesException)
    }

    def 'concurrent: remove - removeStudentStep; anonymize; remove - resume'() {
        given: 'remove pauses at removeStudentStep (READ_COURSE still held)'
        removeStudentFunctionality.executeUntilStep('removeStudentStep', unitOfWork2)

        when: 'anonymize tries to run fully — still blocked by READ_COURSE'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userDto.aggregateId)
        then: 'aggregate is being used in another saga'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'remove resumes and commits'
        removeStudentFunctionality.resumeWorkflow(unitOfWork2)
        then: 'student removed; lock released'
        courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.aggregateId).students.isEmpty()
        sagaStateOf(courseExecutionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        when: 'anonymize is retried — student no longer enrolled'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userDto.aggregateId)
        then: 'student not found in execution → exception'
        thrown(QuizzesException)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
