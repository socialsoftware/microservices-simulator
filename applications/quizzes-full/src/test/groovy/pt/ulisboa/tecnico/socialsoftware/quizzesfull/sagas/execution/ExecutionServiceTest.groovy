package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.execution

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionCourse
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class ExecutionServiceTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String ACRONYM_2 = "DS01"
    public static final String ACADEMIC_TERM_2 = "2nd Semester 2024/2025"
    public static final String USER_USERNAME_2 = "janedoe"
    public static final String NEW_NAME = "John Updated"

    def "createExecution: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §4 Execution — CreateExecution postconditions
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)

        when:
        def dto = executionService.createExecution(ACRONYM_1, ACADEMIC_TERM_1, new ExecutionCourse(courseDto),
                unitOfWorkService.createUnitOfWork("createExecution"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = executionService.getExecutionById(dto.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.acronym == ACRONYM_1
        readBack.academicTerm == ACADEMIC_TERM_1
        readBack.courseId == courseDto.aggregateId
    }

    def "createExecution: NO_DUPLICATE_COURSE_EXECUTION violation"() {
        // Spec: plan.md §4 Execution — rule NO_DUPLICATE_COURSE_EXECUTION (P3 own-table uniqueness)
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionService.createExecution(ACRONYM_1, ACADEMIC_TERM_1, new ExecutionCourse(courseDto),
                unitOfWorkService.createUnitOfWork("createExecution"))

        when:
        executionService.createExecution(ACRONYM_1, ACADEMIC_TERM_1, new ExecutionCourse(courseDto),
                unitOfWorkService.createUnitOfWork("createExecution2"))

        then:
        def ex = thrown(QuizzesFullException)
        ex.errorMessage == QuizzesFullErrorMessage.NO_DUPLICATE_COURSE_EXECUTION
    }

    def "createExecution: different acronym with same academic term is allowed"() {
        // Spec: plan.md §4 Execution — rule NO_DUPLICATE_COURSE_EXECUTION, satisfying-value case
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionService.createExecution(ACRONYM_1, ACADEMIC_TERM_1, new ExecutionCourse(courseDto),
                unitOfWorkService.createUnitOfWork("createExecution"))

        when:
        def dto = executionService.createExecution(ACRONYM_2, ACADEMIC_TERM_1, new ExecutionCourse(courseDto),
                unitOfWorkService.createUnitOfWork("createExecution2"))

        then:
        dto.aggregateId != null
        dto.acronym == ACRONYM_2
    }

    def "getExecutionById: not found throws SimulatorException"() {
        // Spec: plan.md §4 Execution — GetExecutionById not-found path (Path A: aggregateLoadAndRegisterRead)
        when:
        executionService.getExecutionById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getExecutionById"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "updateExecution: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §4 Execution — UpdateExecution postconditions
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def existing = executionService.createExecution(ACRONYM_1, ACADEMIC_TERM_1, new ExecutionCourse(courseDto),
                unitOfWorkService.createUnitOfWork("createExecution"))

        when:
        executionService.updateExecution(existing.aggregateId, ACRONYM_2, ACADEMIC_TERM_2,
                unitOfWorkService.createUnitOfWork("updateExecution"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = executionService.getExecutionById(existing.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.acronym == ACRONYM_2
        readBack.academicTerm == ACADEMIC_TERM_2
    }

    def "deleteExecution: removes execution, not found via fresh UnitOfWork"() {
        // Spec: plan.md §4 Execution — DeleteExecution postconditions (soft-delete)
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def existing = executionService.createExecution(ACRONYM_1, ACADEMIC_TERM_1, new ExecutionCourse(courseDto),
                unitOfWorkService.createUnitOfWork("createExecution"))

        when:
        executionService.deleteExecution(existing.aggregateId, unitOfWorkService.createUnitOfWork("deleteExecution"))

        and: 'verify execution is no longer retrievable'
        executionService.getExecutionById(existing.aggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "enrollStudentInExecution: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §4 Execution — EnrollStudentInExecution postconditions
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = executionService.createExecution(ACRONYM_1, ACADEMIC_TERM_1, new ExecutionCourse(courseDto),
                unitOfWorkService.createUnitOfWork("createExecution"))
        def userDto = userService.createUser(new UserDto(null, USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE, false),
                unitOfWorkService.createUnitOfWork("createUser"))

        when:
        executionService.enrollStudentInExecution(executionDto.aggregateId, userDto,
                unitOfWorkService.createUnitOfWork("enrollStudentInExecution"))

        then: 'read back through a second, fresh UnitOfWork'
        def student = executionService.getStudentByExecutionIdAndUserId(executionDto.aggregateId, userDto.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        student.userAggregateId == userDto.aggregateId
        student.userName == USER_NAME_1
        student.userUsername == USER_USERNAME_1
    }

    def "enrollStudentInExecution: INACTIVE_USER violation"() {
        // Spec: plan.md §4 Execution — rule INACTIVE_USER (P3 DTO-check guard)
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = executionService.createExecution(ACRONYM_1, ACADEMIC_TERM_1, new ExecutionCourse(courseDto),
                unitOfWorkService.createUnitOfWork("createExecution"))
        def inactiveUserDto = new UserDto(1, USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE, false)

        when:
        executionService.enrollStudentInExecution(executionDto.aggregateId, inactiveUserDto,
                unitOfWorkService.createUnitOfWork("enrollStudentInExecution"))

        then:
        def ex = thrown(QuizzesFullException)
        ex.errorMessage == QuizzesFullErrorMessage.INACTIVE_USER
    }

    def "disenrollStudent: removes student, not found via fresh UnitOfWork"() {
        // Spec: plan.md §4 Execution — DisenrollStudent postconditions
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = executionService.createExecution(ACRONYM_1, ACADEMIC_TERM_1, new ExecutionCourse(courseDto),
                unitOfWorkService.createUnitOfWork("createExecution"))
        def userDto = userService.createUser(new UserDto(null, USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE, false),
                unitOfWorkService.createUnitOfWork("createUser"))
        executionService.enrollStudentInExecution(executionDto.aggregateId, userDto,
                unitOfWorkService.createUnitOfWork("enrollStudentInExecution"))

        when:
        executionService.disenrollStudent(executionDto.aggregateId, userDto.aggregateId,
                unitOfWorkService.createUnitOfWork("disenrollStudent"))

        and: 'verify student is no longer enrolled'
        executionService.getStudentByExecutionIdAndUserId(executionDto.aggregateId, userDto.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        def ex = thrown(QuizzesFullException)
        ex.errorMessage == QuizzesFullErrorMessage.COURSE_EXECUTION_STUDENT_NOT_FOUND
    }

    def "updateStudentNameInExecution: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §4 Execution — UpdateStudentName postconditions (cached name update)
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = executionService.createExecution(ACRONYM_1, ACADEMIC_TERM_1, new ExecutionCourse(courseDto),
                unitOfWorkService.createUnitOfWork("createExecution"))
        def userDto = userService.createUser(new UserDto(null, USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE, false),
                unitOfWorkService.createUnitOfWork("createUser"))
        executionService.enrollStudentInExecution(executionDto.aggregateId, userDto,
                unitOfWorkService.createUnitOfWork("enrollStudentInExecution"))

        when:
        executionService.updateStudentNameInExecution(executionDto.aggregateId, userDto.aggregateId, NEW_NAME,
                unitOfWorkService.createUnitOfWork("updateStudentNameInExecution"))

        then: 'read back through a second, fresh UnitOfWork'
        def student = executionService.getStudentByExecutionIdAndUserId(executionDto.aggregateId, userDto.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        student.userName == NEW_NAME
    }

    def "anonymizeStudentInExecution: name, username set to ANONYMOUS and active false, persisted and readable"() {
        // Spec: plan.md §4 Execution — AnonymizeStudent postconditions (cached anonymization)
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = executionService.createExecution(ACRONYM_1, ACADEMIC_TERM_1, new ExecutionCourse(courseDto),
                unitOfWorkService.createUnitOfWork("createExecution"))
        def userDto = userService.createUser(new UserDto(null, USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE, false),
                unitOfWorkService.createUnitOfWork("createUser"))
        executionService.enrollStudentInExecution(executionDto.aggregateId, userDto,
                unitOfWorkService.createUnitOfWork("enrollStudentInExecution"))

        when:
        executionService.anonymizeStudentInExecution(executionDto.aggregateId, userDto.aggregateId,
                unitOfWorkService.createUnitOfWork("anonymizeStudentInExecution"))

        then: 'read back through a second, fresh UnitOfWork'
        def student = executionService.getStudentByExecutionIdAndUserId(executionDto.aggregateId, userDto.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        student.userName == "ANONYMOUS"
        student.userUsername == "ANONYMOUS"
        student.active == false
    }

    def "getStudentByExecutionIdAndUserId: execution not found throws SimulatorException"() {
        // Spec: plan.md §4 Execution — GetStudentByExecutionIdAndUserId not-found path
        //       (Path A: aggregateLoadAndRegisterRead on executionAggregateId)
        when:
        executionService.getStudentByExecutionIdAndUserId(NONEXISTENT_AGGREGATE_ID, 1,
                unitOfWorkService.createUnitOfWork("getStudentByExecutionIdAndUserId"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "getStudentByExecutionIdAndUserId: student not enrolled throws QuizzesFullException"() {
        // Spec: plan.md §4 Execution — GetStudentByExecutionIdAndUserId not-found path
        //       (Path B: COURSE_EXECUTION_STUDENT_NOT_FOUND when student is absent from the collection)
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = executionService.createExecution(ACRONYM_1, ACADEMIC_TERM_1, new ExecutionCourse(courseDto),
                unitOfWorkService.createUnitOfWork("createExecution"))

        when:
        executionService.getStudentByExecutionIdAndUserId(executionDto.aggregateId, 1,
                unitOfWorkService.createUnitOfWork("getStudentByExecutionIdAndUserId"))

        then:
        def ex = thrown(QuizzesFullException)
        ex.errorMessage == QuizzesFullErrorMessage.COURSE_EXECUTION_STUDENT_NOT_FOUND
    }
}
