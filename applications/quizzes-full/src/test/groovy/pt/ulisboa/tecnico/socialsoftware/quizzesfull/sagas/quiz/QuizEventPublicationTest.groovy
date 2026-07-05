package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.quiz

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.InvalidateQuizEvent

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class QuizEventPublicationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    EventService eventService

    def "removeQuestionFromQuiz publishes InvalidateQuizEvent with correct payload"() {
        // Spec: plan.md §6 Quiz — events published (InvalidateQuizEvent), triggered by DeleteQuestionEvent processing
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        def questionDto = createQuestion(courseDto.aggregateId, [], "Q", "C")
        def quizDto = createQuiz(executionDto.aggregateId, [questionDto.aggregateId])

        when:
        quizService.removeQuestionFromQuiz(quizDto.aggregateId, questionDto.aggregateId,
                unitOfWorkService.createUnitOfWork("removeQuestionFromQuiz"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof InvalidateQuizEvent }
        events.size() == 1
        def event = events[0] as InvalidateQuizEvent
        event.publisherAggregateId == quizDto.aggregateId
    }

    def "invalidateQuiz publishes InvalidateQuizEvent with correct payload"() {
        // Spec: plan.md §6 Quiz — events published (InvalidateQuizEvent), triggered by DeleteCourseExecutionEvent processing
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        def questionDto = createQuestion(courseDto.aggregateId, [], "Q", "C")
        def quizDto = createQuiz(executionDto.aggregateId, [questionDto.aggregateId])

        when:
        quizService.invalidateQuiz(quizDto.aggregateId, unitOfWorkService.createUnitOfWork("invalidateQuiz"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof InvalidateQuizEvent }
        events.size() == 1
        def event = events[0] as InvalidateQuizEvent
        event.publisherAggregateId == quizDto.aggregateId
    }

    def "createQuiz does not publish any event"() {
        // Negative case: CreateQuiz has no events-published entry in plan.md §6 Quiz
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        def countBefore = eventService.getAllEvents().size()

        when:
        createQuiz(executionDto.aggregateId, [])

        then:
        eventService.getAllEvents().size() == countBefore
    }
}
