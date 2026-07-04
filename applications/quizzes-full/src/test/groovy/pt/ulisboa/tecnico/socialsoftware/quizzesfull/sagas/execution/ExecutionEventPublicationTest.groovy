package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteCourseExecutionEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DisenrollStudentFromCourseExecutionEvent

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class ExecutionEventPublicationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    EventService eventService

    def "deleteExecution publishes DeleteCourseExecutionEvent with correct payload"() {
        // Spec: plan.md §4 Execution — events published by DeleteExecution
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)

        when:
        executionService.deleteExecution(executionDto.aggregateId, unitOfWorkService.createUnitOfWork("deleteExecution"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof DeleteCourseExecutionEvent }
        events.size() == 1
        def event = events[0] as DeleteCourseExecutionEvent
        event.publisherAggregateId == executionDto.aggregateId
    }

    def "disenrollStudent publishes DisenrollStudentFromCourseExecutionEvent with correct payload"() {
        // Spec: plan.md §4 Execution — events published by DisenrollStudent
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        def userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        executionFunctionalities.enrollStudentInExecution(executionDto.aggregateId, userDto.aggregateId)

        when:
        executionService.disenrollStudent(executionDto.aggregateId, userDto.aggregateId,
                unitOfWorkService.createUnitOfWork("disenrollStudent"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof DisenrollStudentFromCourseExecutionEvent }
        events.size() == 1
        def event = events[0] as DisenrollStudentFromCourseExecutionEvent
        event.publisherAggregateId == executionDto.aggregateId
        event.userId == userDto.aggregateId
    }

    def "createExecution does not publish any event"() {
        // Negative case: CreateExecution has no events-published entry in plan.md §4 Execution
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def countBefore = eventService.getAllEvents().size()

        when:
        createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)

        then:
        eventService.getAllEvents().size() == countBefore
    }
}
