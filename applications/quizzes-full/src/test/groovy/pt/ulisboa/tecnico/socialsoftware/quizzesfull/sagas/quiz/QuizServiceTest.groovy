package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.quiz

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.Quiz
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizExecution
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizQuestion
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizType

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class QuizServiceTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "createQuiz: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §6 Quiz — CreateQuiz postconditions
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        def questionDto = createQuestion(courseDto.aggregateId, [], "Quiz Question", "Describe sorting.")
        def availableDate = LocalDateTime.now().plusDays(1)
        def conclusionDate = LocalDateTime.now().plusDays(2)
        def resultsDate = LocalDateTime.now().plusDays(3)
        def quizExecution = new QuizExecution(executionDto)
        def questions = [new QuizQuestion(questionDto)] as Set

        when:
        def dto = quizService.createQuiz("Test Quiz", availableDate, conclusionDate, resultsDate,
                QuizType.GENERATED, quizExecution, questions,
                unitOfWorkService.createUnitOfWork("createQuiz"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = quizService.getQuizById(dto.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.title == "Test Quiz"
        readBack.executionId == executionDto.aggregateId
        readBack.questionIds.contains(questionDto.aggregateId)
        readBack.creationDate != null
    }

    def "getQuizById: not found throws SimulatorException"() {
        // Spec: plan.md §6 Quiz — GetQuizById not-found path (Path A: aggregateLoadAndRegisterRead)
        when:
        quizService.getQuizById(NONEXISTENT_AGGREGATE_ID, unitOfWorkService.createUnitOfWork("getQuizById"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "updateQuiz: dates and questions persisted through a fresh UnitOfWork"() {
        // Spec: plan.md §6 Quiz — UpdateQuiz postconditions
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        def questionDto1 = createQuestion(courseDto.aggregateId, [], "Q1", "C1")
        def questionDto2 = createQuestion(courseDto.aggregateId, [], "Q2", "C2")
        def existing = createQuiz(executionDto.aggregateId, [questionDto1.aggregateId])
        def newAvailableDate = LocalDateTime.now().plusDays(2)
        def newConclusionDate = LocalDateTime.now().plusDays(4)
        def newResultsDate = LocalDateTime.now().plusDays(5)
        def newQuestions = [new QuizQuestion(questionDto2)] as Set

        when:
        quizService.updateQuiz(existing.aggregateId, newAvailableDate, newConclusionDate, newResultsDate,
                newQuestions, unitOfWorkService.createUnitOfWork("updateQuiz"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = quizService.getQuizById(existing.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.availableDate == newAvailableDate
        readBack.questionIds.contains(questionDto2.aggregateId)
        !readBack.questionIds.contains(questionDto1.aggregateId)
    }

    def "updateQuiz: not found throws SimulatorException"() {
        // Spec: plan.md §6 Quiz — UpdateQuiz not-found path (Path A: aggregateLoadAndRegisterRead)
        when:
        quizService.updateQuiz(NONEXISTENT_AGGREGATE_ID, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3), [] as Set,
                unitOfWorkService.createUnitOfWork("updateQuiz"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "updateQuestionInQuiz: cached question title/content persisted through a fresh UnitOfWork"() {
        // Spec: plan.md §6 Quiz — QUESTION_EXISTS / UpdateQuestionEvent cached-field update.
        // Event-driven only (called from QuizEventProcessing.processUpdateQuestionEvent with an
        // existing, active quiz id sourced from its own event subscription) — no not-found path
        // is reachable through any legitimate caller, so no not-found case is added here.
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        def questionDto = createQuestion(courseDto.aggregateId, [], "Original Title", "Original Content")
        def quizDto = createQuiz(executionDto.aggregateId, [questionDto.aggregateId])

        when:
        quizService.updateQuestionInQuiz(quizDto.aggregateId, questionDto.aggregateId,
                "Updated Title", "Updated Content", unitOfWorkService.createUnitOfWork("updateQuestionInQuiz"))

        then: 'read back through a second, fresh UnitOfWork (QuizDto has no cached per-question fields)'
        def uow = unitOfWorkService.createUnitOfWork("check")
        Quiz readBack = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizDto.aggregateId, uow)
        readBack.questions.any {
            it.questionAggregateId == questionDto.aggregateId &&
            it.title == "Updated Title" && it.content == "Updated Content"
        }
    }

    def "removeQuestionFromQuiz: quiz invalidated (soft-deleted), not found via fresh UnitOfWork"() {
        // Spec: plan.md §6 Quiz — QUESTION_EXISTS / DeleteQuestionEvent self-invalidation.
        // Event-driven only — no not-found path reachable through any legitimate caller.
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        def questionDto = createQuestion(courseDto.aggregateId, [], "Q", "C")
        def quizDto = createQuiz(executionDto.aggregateId, [questionDto.aggregateId])

        when:
        quizService.removeQuestionFromQuiz(quizDto.aggregateId, questionDto.aggregateId,
                unitOfWorkService.createUnitOfWork("removeQuestionFromQuiz"))

        and: 'verify quiz is no longer retrievable'
        quizService.getQuizById(quizDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "invalidateQuiz: quiz invalidated (soft-deleted), not found via fresh UnitOfWork"() {
        // Spec: plan.md §6 Quiz — COURSE_EXECUTION_EXISTS / DeleteCourseExecutionEvent self-invalidation.
        // Event-driven only — no not-found path reachable through any legitimate caller.
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        def questionDto = createQuestion(courseDto.aggregateId, [], "Q", "C")
        def quizDto = createQuiz(executionDto.aggregateId, [questionDto.aggregateId])

        when:
        quizService.invalidateQuiz(quizDto.aggregateId, unitOfWorkService.createUnitOfWork("invalidateQuiz"))

        and: 'verify quiz is no longer retrievable'
        quizService.getQuizById(quizDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }
}
