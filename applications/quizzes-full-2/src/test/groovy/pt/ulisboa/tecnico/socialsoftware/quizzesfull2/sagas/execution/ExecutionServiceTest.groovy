package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteCourseExecutionEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DisenrollStudentFromCourseExecutionEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto

@DataJpaTest
@Transactional
@Import(ExecutionServiceTest.LocalBeanConfiguration)
class ExecutionServiceTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999
    public static final String OTHER_EXECUTION_ACRONYM = "SE-02"
    public static final String OTHER_EXECUTION_ACADEMIC_TERM = "2026/2027"

    @Autowired
    EventService eventService

    def "getExecutionById: reads back the persisted execution through a fresh UnitOfWork"() {
        // Spec: plan.md §4 Execution — GetExecutionById(executionAggregateId)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)

        when:
        flushAndClear()
        def result = executionService.getExecutionById(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == executionAggregateId
        result.courseAggregateId == courseAggregateId
        result.courseName == COURSE_NAME
        result.courseType == COURSE_TYPE
        result.acronym == EXECUTION_ACRONYM
        result.academicTerm == EXECUTION_ACADEMIC_TERM
        result.endDate == EXECUTION_END_DATE
        result.students.isEmpty()
        result.version != null
    }

    def "getExecutionById: unknown aggregate id is not found"() {
        // Spec: plan.md §4 Execution — Path A (aggregateLoadAndRegisterRead)
        when:
        executionService.getExecutionById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "getExecutions: returns every execution with its own state"() {
        // Spec: plan.md §4 Execution — GetExecutions()
        given:
        def courseAggregateId = createCourse()
        def firstAggregateId = createExecution(courseAggregateId, EXECUTION_ACRONYM)
        def secondAggregateId = createExecution(courseAggregateId, OTHER_EXECUTION_ACRONYM)

        when:
        flushAndClear()
        def result = executionService.getExecutions(unitOfWorkService.createUnitOfWork("check"))

        then:
        result.size() == 2
        def first = result.find { it.aggregateId == firstAggregateId }
        first.acronym == EXECUTION_ACRONYM
        first.courseAggregateId == courseAggregateId
        def second = result.find { it.aggregateId == secondAggregateId }
        second.acronym == OTHER_EXECUTION_ACRONYM
        second.academicTerm == EXECUTION_ACADEMIC_TERM
    }

    def "getExecutions: returns an empty list when no execution exists"() {
        // Spec: plan.md §4 Execution — GetExecutions()
        when:
        def result = executionService.getExecutions(unitOfWorkService.createUnitOfWork("check"))

        then:
        result.isEmpty()
    }

    def "getUserExecutions: returns the executions the user is enrolled in"() {
        // Spec: plan.md §4 Execution — GetUserExecutions(userAggregateId)
        given:
        def courseAggregateId = createCourse()
        def firstAggregateId = createExecution(courseAggregateId, EXECUTION_ACRONYM)
        def secondAggregateId = createExecution(courseAggregateId, OTHER_EXECUTION_ACRONYM)
        def studentAggregateId = createActiveUser(EXECUTION_STUDENT_USER_NAME, EXECUTION_STUDENT_USER_USERNAME)
        enrollStudentInExecution(firstAggregateId, studentAggregateId)
        enrollStudentInExecution(secondAggregateId, studentAggregateId)

        when:
        flushAndClear()
        def result = executionService.getUserExecutions(studentAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.size() == 2
        result.collect { it.aggregateId } as Set == [firstAggregateId, secondAggregateId] as Set
        result.every { it.students.collect { student -> student.userAggregateId } == [studentAggregateId] }
    }

    def "getUserExecutions: excludes executions the user is not enrolled in"() {
        // Spec: plan.md §4 Execution — GetUserExecutions(userAggregateId) filters on the enrolled student
        given:
        def courseAggregateId = createCourse()
        def enrolledAggregateId = createExecution(courseAggregateId, EXECUTION_ACRONYM)
        def otherAggregateId = createExecution(courseAggregateId, OTHER_EXECUTION_ACRONYM)
        def studentAggregateId = createActiveUser(EXECUTION_STUDENT_USER_NAME, EXECUTION_STUDENT_USER_USERNAME)
        def otherStudentAggregateId = createActiveUser()
        enrollStudentInExecution(enrolledAggregateId, studentAggregateId)
        enrollStudentInExecution(otherAggregateId, otherStudentAggregateId)

        when:
        flushAndClear()
        def result = executionService.getUserExecutions(studentAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.collect { it.aggregateId } == [enrolledAggregateId]
    }

    def "getUserExecutions: returns an empty list when the user is enrolled nowhere"() {
        // Spec: plan.md §4 Execution — GetUserExecutions(userAggregateId)
        given:
        def courseAggregateId = createCourse()
        createExecution(courseAggregateId)

        when:
        flushAndClear()
        def result = executionService.getUserExecutions(EXECUTION_STUDENT_USER_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.isEmpty()
    }

    def "createExecution: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §4 Execution — CreateExecution(courseAggregateId, acronym, academicTerm, endDate)
        given:
        def courseAggregateId = createCourse()

        when:
        def created = executionService.createExecution(
                executionDtoOf(EXECUTION_ACRONYM, EXECUTION_ACADEMIC_TERM),
                courseDtoOf(courseAggregateId),
                unitOfWorkService.createUnitOfWork("createExecution"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = executionService.getExecutionById(created.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.aggregateId == created.aggregateId
        readBack.acronym == EXECUTION_ACRONYM
        readBack.academicTerm == EXECUTION_ACADEMIC_TERM
        readBack.endDate == EXECUTION_END_DATE
        readBack.courseAggregateId == courseAggregateId
        readBack.courseName == COURSE_NAME
        readBack.courseType == COURSE_TYPE
        readBack.students.isEmpty()
    }

    def "createExecution: NO_DUPLICATE_COURSE_EXECUTION violation"() {
        // Spec: plan.md §3.2 — rule NO_DUPLICATE_COURSE_EXECUTION (P3 own-table uniqueness)
        given: 'an execution with the same (acronym, academicTerm) pair already exists'
        def courseAggregateId = createCourse()
        createExecution(courseAggregateId, EXECUTION_ACRONYM, EXECUTION_ACADEMIC_TERM)

        when:
        executionService.createExecution(
                executionDtoOf(EXECUTION_ACRONYM, EXECUTION_ACADEMIC_TERM),
                courseDtoOf(courseAggregateId),
                unitOfWorkService.createUnitOfWork("createExecution"))

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.NO_DUPLICATE_COURSE_EXECUTION
    }

    def "createExecution: the same acronym in a different academic term is allowed"() {
        // Spec: plan.md §3.2 — NO_DUPLICATE_COURSE_EXECUTION constrains the (acronym, academicTerm) pair
        given:
        def courseAggregateId = createCourse()
        createExecution(courseAggregateId, EXECUTION_ACRONYM, EXECUTION_ACADEMIC_TERM)

        when:
        def created = executionService.createExecution(
                executionDtoOf(EXECUTION_ACRONYM, OTHER_EXECUTION_ACADEMIC_TERM),
                courseDtoOf(courseAggregateId),
                unitOfWorkService.createUnitOfWork("createExecution"))

        then:
        flushAndClear()
        def readBack = executionService.getExecutionById(created.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.acronym == EXECUTION_ACRONYM
        readBack.academicTerm == OTHER_EXECUTION_ACADEMIC_TERM
    }

    def "updateExecution: the new acronym and academic term are persisted"() {
        // Spec: plan.md §4 Execution — UpdateExecution(executionAggregateId, acronym, academicTerm)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)

        when:
        executionService.updateExecution(executionAggregateId, OTHER_EXECUTION_ACRONYM,
                OTHER_EXECUTION_ACADEMIC_TERM, unitOfWorkService.createUnitOfWork("updateExecution"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = executionService.getExecutionById(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.acronym == OTHER_EXECUTION_ACRONYM
        readBack.academicTerm == OTHER_EXECUTION_ACADEMIC_TERM
    }

    def "updateExecution: leaves the course snapshot, end date and roster untouched"() {
        // Spec: plan.md §4 Execution — UpdateExecution only updates acronym or academic term
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def studentAggregateId = createActiveUser(EXECUTION_STUDENT_USER_NAME, EXECUTION_STUDENT_USER_USERNAME)
        enrollStudentInExecution(executionAggregateId, studentAggregateId)

        when:
        executionService.updateExecution(executionAggregateId, OTHER_EXECUTION_ACRONYM,
                OTHER_EXECUTION_ACADEMIC_TERM, unitOfWorkService.createUnitOfWork("updateExecution"))

        then:
        flushAndClear()
        def readBack = executionService.getExecutionById(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.courseAggregateId == courseAggregateId
        readBack.courseName == COURSE_NAME
        readBack.courseType == COURSE_TYPE
        readBack.endDate == EXECUTION_END_DATE
        readBack.students.collect { it.userAggregateId } == [studentAggregateId]
    }

    def "updateExecution: unknown aggregate id is not found"() {
        // Spec: plan.md §4 Execution — Path A (aggregateLoadAndRegisterRead)
        when:
        executionService.updateExecution(NONEXISTENT_AGGREGATE_ID, OTHER_EXECUTION_ACRONYM,
                OTHER_EXECUTION_ACADEMIC_TERM, unitOfWorkService.createUnitOfWork("updateExecution"))

        then:
        thrown(SimulatorException)
    }

    def "enrollStudent: the student snapshot is added to the roster and persisted"() {
        // Spec: plan.md §4 Execution — EnrollStudentInExecution(executionAggregateId, userAggregateId)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)

        when:
        executionService.enrollStudent(executionAggregateId, activeUserDto(),
                unitOfWorkService.createUnitOfWork("enrollStudent"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = executionService.getExecutionById(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.students.size() == 1
        def student = readBack.students[0]
        student.userAggregateId == EXECUTION_STUDENT_USER_AGGREGATE_ID
        student.userName == EXECUTION_STUDENT_USER_NAME
        student.userUsername == EXECUTION_STUDENT_USER_USERNAME
        student.userVersion == EXECUTION_STUDENT_USER_VERSION
        student.isActive()
    }

    def "enrollStudent: a second student joins the existing roster"() {
        // Spec: plan.md §4 Execution — EnrollStudentInExecution(executionAggregateId, userAggregateId)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        executionService.enrollStudent(executionAggregateId, activeUserDto(),
                unitOfWorkService.createUnitOfWork("enrollStudent"))

        when:
        executionService.enrollStudent(executionAggregateId,
                activeUserDto(EXECUTION_STUDENT_USER_AGGREGATE_ID_2),
                unitOfWorkService.createUnitOfWork("enrollStudent"))

        then:
        flushAndClear()
        def readBack = executionService.getExecutionById(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.students.collect { it.userAggregateId } as Set ==
                [EXECUTION_STUDENT_USER_AGGREGATE_ID, EXECUTION_STUDENT_USER_AGGREGATE_ID_2] as Set
    }

    def "enrollStudent: INACTIVE_USER violation"() {
        // Spec: plan.md §3.2 — rule INACTIVE_USER (P3 DTO field validation on userDto.isActive())
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def inactiveUserDto = activeUserDto()
        inactiveUserDto.setActive(false)

        when:
        executionService.enrollStudent(executionAggregateId, inactiveUserDto,
                unitOfWorkService.createUnitOfWork("enrollStudent"))

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.INACTIVE_USER
    }

    def "enrollStudent: unknown aggregate id is not found"() {
        // Spec: plan.md §4 Execution — Path A (aggregateLoadAndRegisterRead)
        when:
        executionService.enrollStudent(NONEXISTENT_AGGREGATE_ID, activeUserDto(),
                unitOfWorkService.createUnitOfWork("enrollStudent"))

        then:
        thrown(SimulatorException)
    }

    def "disenrollStudent: the student is removed from the roster and the removal is persisted"() {
        // Spec: plan.md §4 Execution — DisenrollStudent(executionAggregateId, userAggregateId)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        executionService.enrollStudent(executionAggregateId, activeUserDto(),
                unitOfWorkService.createUnitOfWork("enrollStudent"))

        when:
        executionService.disenrollStudent(executionAggregateId, EXECUTION_STUDENT_USER_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("disenrollStudent"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = executionService.getExecutionById(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.students.isEmpty()
    }

    def "disenrollStudent: only the named student leaves the roster"() {
        // Spec: plan.md §4 Execution — DisenrollStudent removes one student from the execution
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        executionService.enrollStudent(executionAggregateId, activeUserDto(),
                unitOfWorkService.createUnitOfWork("enrollStudent"))
        executionService.enrollStudent(executionAggregateId,
                activeUserDto(EXECUTION_STUDENT_USER_AGGREGATE_ID_2),
                unitOfWorkService.createUnitOfWork("enrollStudent"))

        when:
        executionService.disenrollStudent(executionAggregateId, EXECUTION_STUDENT_USER_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("disenrollStudent"))

        then:
        flushAndClear()
        def readBack = executionService.getExecutionById(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.students.collect { it.userAggregateId } == [EXECUTION_STUDENT_USER_AGGREGATE_ID_2]
        readBack.acronym == EXECUTION_ACRONYM
        readBack.academicTerm == EXECUTION_ACADEMIC_TERM
    }

    def "disenrollStudent: unknown aggregate id is not found"() {
        // Spec: plan.md §4 Execution — Path A (aggregateLoadAndRegisterRead)
        when:
        executionService.disenrollStudent(NONEXISTENT_AGGREGATE_ID, EXECUTION_STUDENT_USER_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("disenrollStudent"))

        then:
        thrown(SimulatorException)
    }

    def "disenrollStudent publishes DisenrollStudentFromCourseExecutionEvent with correct payload"() {
        // Spec: plan.md §4 Execution — events published: DisenrollStudentFromCourseExecutionEvent
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        executionService.enrollStudent(executionAggregateId, activeUserDto(),
                unitOfWorkService.createUnitOfWork("enrollStudent"))

        when:
        executionService.disenrollStudent(executionAggregateId, EXECUTION_STUDENT_USER_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("disenrollStudent"))

        then:
        def events = eventService.getAllEvents()
                .findAll { it instanceof DisenrollStudentFromCourseExecutionEvent }
        events.size() == 1
        def event = events[0] as DisenrollStudentFromCourseExecutionEvent
        event.publisherAggregateId == executionAggregateId
        event.executionAggregateId == executionAggregateId
        event.studentAggregateId == EXECUTION_STUDENT_USER_AGGREGATE_ID
    }

    def "enrollStudent publishes no DisenrollStudentFromCourseExecutionEvent"() {
        // Spec: plan.md §4 Execution — DisenrollStudentFromCourseExecutionEvent is published by DisenrollStudent only
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def countBefore = eventService.getAllEvents().size()

        when:
        executionService.enrollStudent(executionAggregateId, activeUserDto(),
                unitOfWorkService.createUnitOfWork("enrollStudent"))

        then:
        eventService.getAllEvents().size() == countBefore
    }

    def "deleteExecution: the execution is soft-deleted and no longer resolvable"() {
        // Spec: plan.md §4 Execution — DeleteExecution(executionAggregateId)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)

        when:
        executionService.deleteExecution(executionAggregateId,
                unitOfWorkService.createUnitOfWork("deleteExecution"))

        and: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        executionService.getExecutionById(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then: 'a DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

    def "deleteExecution: clears the roster in the same operation so REMOVE_NO_STUDENTS holds"() {
        // Spec: plan.md §3.2 — rule REMOVE_NO_STUDENTS (P1); DeleteExecution must clear the roster
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        executionService.enrollStudent(executionAggregateId, activeUserDto(),
                unitOfWorkService.createUnitOfWork("enrollStudent"))

        when:
        executionService.deleteExecution(executionAggregateId,
                unitOfWorkService.createUnitOfWork("deleteExecution"))

        then: 'the P1 invariant did not fire — the roster was emptied in the same operation'
        notThrown(QuizzesFull2Exception)

        and: 'the deleted execution is gone from the active executions and from the student roster query'
        flushAndClear()
        executionService.getExecutions(unitOfWorkService.createUnitOfWork("check")).isEmpty()
        executionService.getUserExecutions(EXECUTION_STUDENT_USER_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("check")).isEmpty()
    }

    def "deleteExecution: leaves the other executions untouched"() {
        // Spec: plan.md §4 Execution — DeleteExecution targets a single execution
        given:
        def courseAggregateId = createCourse()
        def deletedAggregateId = createExecution(courseAggregateId, EXECUTION_ACRONYM)
        def survivingAggregateId = createExecution(courseAggregateId, OTHER_EXECUTION_ACRONYM)

        when:
        executionService.deleteExecution(deletedAggregateId,
                unitOfWorkService.createUnitOfWork("deleteExecution"))

        then:
        flushAndClear()
        def remaining = executionService.getExecutions(unitOfWorkService.createUnitOfWork("check"))
        remaining.collect { it.aggregateId } == [survivingAggregateId]
        remaining[0].acronym == OTHER_EXECUTION_ACRONYM
    }

    def "deleteExecution: unknown aggregate id is not found"() {
        // Spec: plan.md §4 Execution — Path A (aggregateLoadAndRegisterRead)
        when:
        executionService.deleteExecution(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("deleteExecution"))

        then:
        thrown(SimulatorException)
    }

    def "deleteExecution publishes DeleteCourseExecutionEvent with correct payload"() {
        // Spec: plan.md §4 Execution — events published: DeleteCourseExecutionEvent
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)

        when:
        executionService.deleteExecution(executionAggregateId,
                unitOfWorkService.createUnitOfWork("deleteExecution"))

        then:
        def events = eventService.getAllEvents()
                .findAll { it instanceof DeleteCourseExecutionEvent }
        events.size() == 1
        def event = events[0] as DeleteCourseExecutionEvent
        event.publisherAggregateId == executionAggregateId
        event.executionAggregateId == executionAggregateId
    }

    private static UserDto activeUserDto(Integer userAggregateId = EXECUTION_STUDENT_USER_AGGREGATE_ID) {
        def userDto = new UserDto()
        userDto.setAggregateId(userAggregateId)
        userDto.setVersion(EXECUTION_STUDENT_USER_VERSION)
        userDto.setName(EXECUTION_STUDENT_USER_NAME)
        userDto.setUsername(EXECUTION_STUDENT_USER_USERNAME)
        userDto.setRole(USER_ROLE)
        userDto.setActive(true)
        return userDto
    }

    private static ExecutionDto executionDtoOf(String acronym, String academicTerm) {
        def executionDto = new ExecutionDto()
        executionDto.setAcronym(acronym)
        executionDto.setAcademicTerm(academicTerm)
        executionDto.setEndDate(EXECUTION_END_DATE)
        return executionDto
    }

    private static CourseDto courseDtoOf(Integer courseAggregateId) {
        def courseDto = new CourseDto()
        courseDto.setAggregateId(courseAggregateId)
        courseDto.setName(COURSE_NAME)
        courseDto.setType(COURSE_TYPE)
        return courseDto
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
