package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.quiz

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizExecution
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizQuestion
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizType
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.sagas.SagaQuiz
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionDto

import java.time.LocalDateTime

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.QUIZ_DATE_ORDERING
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class QuizIntraInvariantTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static QuizExecution makeExecution(int id) {
        def dto = new ExecutionDto()
        dto.setAggregateId(id)
        dto.setVersion(1L)
        return new QuizExecution(dto)
    }

    private static QuizQuestion makeQuestion(int id) {
        def dto = new QuestionDto()
        dto.setAggregateId(id)
        dto.setVersion(1L)
        dto.setTitle("What is 2+2?")
        dto.setContent("Choose the correct answer.")
        return new QuizQuestion(dto)
    }

    // ─── Creation happy-path ──────────────────────────────────────────────────

    def "create quiz"() {
        given:
        def available = LocalDateTime.now().plusDays(1)
        def conclusion = LocalDateTime.now().plusDays(2)
        def results = LocalDateTime.now().plusDays(3)

        when:
        def quiz = new SagaQuiz(1, "Sample Quiz", available, conclusion, results,
                QuizType.TEST, makeExecution(100), [makeQuestion(200)] as Set)

        then:
        quiz.title == "Sample Quiz"
        quiz.creationDate != null
        quiz.availableDate == available
        quiz.conclusionDate == conclusion
        quiz.resultsDate == results
        quiz.quizType == QuizType.TEST
        quiz.quizExecution.executionAggregateId == 100
        quiz.quizExecution.executionVersion == 1L
        quiz.questions.size() == 1
        quiz.questions.first().questionAggregateId == 200
        quiz.questions.first().title == "What is 2+2?"
    }

    // ─── P1: QUIZ_DATE_ORDERING ───────────────────────────────────────────────
    // Spec: creationDate < availableDate < conclusionDate <= resultsDate.
    // creationDate is stamped by the constructor (LocalDateTime.now()); availableDate,
    // conclusionDate and resultsDate are pinned exactly via direct construction.

    def "QUIZ_DATE_ORDERING: valid dates pass invariant"() {
        given:
        def available = LocalDateTime.now().plusDays(1)
        def conclusion = LocalDateTime.now().plusDays(2)
        def results = LocalDateTime.now().plusDays(3)
        def quiz = new SagaQuiz(1, "Quiz", available, conclusion, results,
                QuizType.TEST, makeExecution(100), [makeQuestion(200)] as Set)

        when:
        quiz.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }

    def "QUIZ_DATE_ORDERING: availableDate after conclusionDate violates (far equivalence-partition)"() {
        given:
        def available = LocalDateTime.now().plusDays(5)
        def conclusion = LocalDateTime.now().plusDays(2)
        def results = LocalDateTime.now().plusDays(6)
        def quiz = new SagaQuiz(1, "Quiz", available, conclusion, results,
                QuizType.TEST, makeExecution(100), [makeQuestion(200)] as Set)

        when:
        quiz.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == QUIZ_DATE_ORDERING
    }

    // ─── BVA: availableDate < conclusionDate (strict) ────────────────────────
    // On-point : availableDate one nanosecond before conclusionDate — satisfies strict <.
    // Off-point: availableDate == conclusionDate — equal is NOT strictly less-than, violates.

    def "QUIZ_DATE_ORDERING boundary — availableDate one nanosecond before conclusionDate satisfies (on-point)"() {
        given: 'available is exactly one nanosecond before conclusion (on-point of strict <)'
        def conclusion = LocalDateTime.now().plusDays(2)
        def available = conclusion.minusNanos(1)
        def results = LocalDateTime.now().plusDays(3)
        def quiz = new SagaQuiz(1, "Quiz", available, conclusion, results,
                QuizType.TEST, makeExecution(100), [makeQuestion(200)] as Set)

        when:
        quiz.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }

    def "QUIZ_DATE_ORDERING boundary — availableDate == conclusionDate violates (off-point)"() {
        given: 'available equals conclusion (off-point of strict <)'
        def available = LocalDateTime.now().plusDays(2)
        def conclusion = available
        def results = LocalDateTime.now().plusDays(3)
        def quiz = new SagaQuiz(1, "Quiz", available, conclusion, results,
                QuizType.TEST, makeExecution(100), [makeQuestion(200)] as Set)

        when:
        quiz.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == QUIZ_DATE_ORDERING
    }

    // ─── BVA: conclusionDate <= resultsDate (inclusive) ───────────────────────
    // On-point : conclusionDate == resultsDate — equality satisfies <=.
    // Off-point: resultsDate one nanosecond before conclusionDate — violates <=.

    def "QUIZ_DATE_ORDERING boundary — conclusionDate == resultsDate satisfies (on-point)"() {
        given: 'conclusion equals results (on-point of inclusive <=)'
        def available = LocalDateTime.now().plusDays(1)
        def conclusion = LocalDateTime.now().plusDays(2)
        def results = conclusion
        def quiz = new SagaQuiz(1, "Quiz", available, conclusion, results,
                QuizType.TEST, makeExecution(100), [makeQuestion(200)] as Set)

        when:
        quiz.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }

    def "QUIZ_DATE_ORDERING boundary — resultsDate one nanosecond before conclusionDate violates (off-point)"() {
        given: 'results is one nanosecond before conclusion (off-point of inclusive <=)'
        def available = LocalDateTime.now().plusDays(1)
        def conclusion = LocalDateTime.now().plusDays(2)
        def results = conclusion.minusNanos(1)
        def quiz = new SagaQuiz(1, "Quiz", available, conclusion, results,
                QuizType.TEST, makeExecution(100), [makeQuestion(200)] as Set)

        when:
        quiz.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == QUIZ_DATE_ORDERING
    }

    // ─── P1: QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE ───────────────────────────
    // Spec: once lastModifiedTime > prev.availableDate the dates and questions must equal prev.
    // Predicate: lastModifiedTime.isAfter(prev.availableDate) ⟹ dates/questions unchanged.
    // setPrev(Aggregate) is public on Aggregate; setLastModifiedTime(LocalDateTime) is public
    // on Quiz — both can be pinned directly without the versioning/merge machinery.
    //
    // BVA boundary: lastModifiedTime vs prev.availableDate
    // On-point : lastModifiedTime == prev.availableDate  (isAfter is false → guard not entered → passes)
    // Off-point: lastModifiedTime == prev.availableDate + 1 ns AND a date changed → guard entered → violates

    def "QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE boundary — lastModifiedTime == prev.availableDate satisfies (on-point)"() {
        given: 'a prev quiz with a pinned availableDate'
        def available = LocalDateTime.of(2030, 6, 1, 10, 0, 0, 0)
        def conclusion = LocalDateTime.of(2030, 6, 2, 10, 0, 0, 0)
        def results = LocalDateTime.of(2030, 6, 3, 10, 0, 0, 0)
        def prev = new SagaQuiz(1, "Quiz", available, conclusion, results,
                QuizType.TEST, makeExecution(100), [makeQuestion(200)] as Set)

        and: 'a copy with lastModifiedTime == prev.availableDate (on-point: isAfter is false)'
        def current = new SagaQuiz(prev)
        current.setPrev(prev)
        current.setLastModifiedTime(available)  // exactly equal — not after

        when:
        current.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }

    def "QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE boundary — lastModifiedTime one nanosecond after prev.availableDate with changed date violates (off-point)"() {
        given: 'a prev quiz with a pinned availableDate'
        def available = LocalDateTime.of(2030, 6, 1, 10, 0, 0, 0)
        def conclusion = LocalDateTime.of(2030, 6, 2, 10, 0, 0, 0)
        def results = LocalDateTime.of(2030, 6, 3, 10, 0, 0, 0)
        def prev = new SagaQuiz(1, "Quiz", available, conclusion, results,
                QuizType.TEST, makeExecution(100), [makeQuestion(200)] as Set)

        and: 'a copy with lastModifiedTime one nanosecond after prev.availableDate (off-point: isAfter is true) and a changed conclusionDate'
        def current = new SagaQuiz(prev)
        current.setPrev(prev)
        // setConclusionDate re-stamps lastModifiedTime, so reset it to the off-point value after
        current.setConclusionDate(conclusion.plusDays(1))
        current.setLastModifiedTime(available.plusNanos(1))

        when:
        current.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE
    }
}
