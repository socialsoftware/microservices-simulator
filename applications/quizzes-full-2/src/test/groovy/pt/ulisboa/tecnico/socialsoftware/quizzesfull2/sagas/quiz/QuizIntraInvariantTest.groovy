package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.quiz

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestion
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.sagas.SagaQuiz

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(QuizIntraInvariantTest.LocalBeanConfiguration)
class QuizIntraInvariantTest extends QuizzesFull2SpockTest {

    private static SagaQuiz aQuiz(LocalDateTime availableDate = QUIZ_AVAILABLE_DATE,
                                  LocalDateTime conclusionDate = QUIZ_CONCLUSION_DATE,
                                  LocalDateTime resultsDate = QUIZ_RESULTS_DATE) {
        return new SagaQuiz(QUIZ_AGGREGATE_ID, EXECUTION_AGGREGATE_ID, QUIZ_EXECUTION_VERSION, QUIZ_TITLE,
                QUIZ_CREATION_DATE, availableDate, conclusionDate, resultsDate, QUIZ_TYPE)
    }

    private static QuizQuestion aQuestion(Integer questionAggregateId = QUIZ_QUESTION_AGGREGATE_ID) {
        return new QuizQuestion(questionAggregateId, QUIZ_QUESTION_VERSION, QUIZ_QUESTION_TITLE, QUIZ_QUESTION_CONTENT)
    }

    // A version whose prev is an available quiz: the copy constructor carries prev, and
    // lastModifiedTime is pinned by the caller because the mutators stamp DateHandler.now().
    private static SagaQuiz aNextVersionOf(SagaQuiz quiz) {
        return new SagaQuiz(quiz)
    }

    def "create quiz"() {
        // Spec: plan.md §6 Quiz — CreateQuiz(executionAggregateId, title, availableDate, conclusionDate,
        // resultsDate, quizType, questionAggregateIds); snapshot QuizExecution (plan.md § snapshot table)
        when:
        def quiz = aQuiz()
        quiz.verifyInvariants()

        then:
        quiz.aggregateId == QUIZ_AGGREGATE_ID
        quiz.title == QUIZ_TITLE
        quiz.creationDate == QUIZ_CREATION_DATE
        quiz.availableDate == QUIZ_AVAILABLE_DATE
        quiz.conclusionDate == QUIZ_CONCLUSION_DATE
        quiz.resultsDate == QUIZ_RESULTS_DATE
        quiz.quizType == QUIZ_TYPE
        quiz.execution.executionAggregateId == EXECUTION_AGGREGATE_ID
        quiz.execution.executionVersion == QUIZ_EXECUTION_VERSION
        quiz.execution.quiz.is(quiz)
        quiz.questions.isEmpty()
        quiz.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    def "quiz: QUIZ_DATE_ORDERING violation — available date not after creation date"() {
        // Spec: plan.md §3.1 rule QUIZ_DATE_ORDERING — creationDate < availableDate
        given:
        def quiz = aQuiz(QUIZ_CREATION_DATE)

        when:
        quiz.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.QUIZ_DATE_ORDERING
    }

    def "quiz: QUIZ_DATE_ORDERING on-point — available date one tick after creation date"() {
        // Spec: plan.md §3.1 rule QUIZ_DATE_ORDERING — boundary straddle on creationDate < availableDate
        given:
        def quiz = aQuiz(QUIZ_CREATION_DATE.plusNanos(1))

        when:
        quiz.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "quiz: QUIZ_DATE_ORDERING violation — conclusion date equal to available date"() {
        // Spec: plan.md §3.1 rule QUIZ_DATE_ORDERING — availableDate < conclusionDate
        given:
        def quiz = aQuiz(QUIZ_AVAILABLE_DATE, QUIZ_AVAILABLE_DATE)

        when:
        quiz.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.QUIZ_DATE_ORDERING
    }

    def "quiz: QUIZ_DATE_ORDERING on-point — conclusion date one tick after available date"() {
        // Spec: plan.md §3.1 rule QUIZ_DATE_ORDERING — boundary straddle on availableDate < conclusionDate
        given:
        def quiz = aQuiz(QUIZ_AVAILABLE_DATE, QUIZ_AVAILABLE_DATE.plusNanos(1))

        when:
        quiz.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "quiz: QUIZ_DATE_ORDERING violation — results date one tick before conclusion date"() {
        // Spec: plan.md §3.1 rule QUIZ_DATE_ORDERING — conclusionDate <= resultsDate
        given:
        def quiz = aQuiz(QUIZ_AVAILABLE_DATE, QUIZ_CONCLUSION_DATE, QUIZ_CONCLUSION_DATE.minusNanos(1))

        when:
        quiz.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.QUIZ_DATE_ORDERING
    }

    def "quiz: QUIZ_DATE_ORDERING on-point — results date equal to conclusion date"() {
        // Spec: plan.md §3.1 rule QUIZ_DATE_ORDERING — boundary straddle on conclusionDate <= resultsDate
        given:
        def quiz = aQuiz(QUIZ_AVAILABLE_DATE, QUIZ_CONCLUSION_DATE, QUIZ_CONCLUSION_DATE)

        when:
        quiz.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "quiz: QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE violation — conclusion date changed after the quiz became available"() {
        // Spec: plan.md §3.1 rule QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE —
        // lastModifiedTime > prev.availableDate implies the dates and questions are unchanged
        given:
        def quiz = aNextVersionOf(aQuiz())
        quiz.setConclusionDate(QUIZ_CONCLUSION_DATE.plusHours(1))
        quiz.setLastModifiedTime(QUIZ_AVAILABLE_DATE.plusNanos(1))

        when:
        quiz.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE
    }

    def "quiz: QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE on-point — conclusion date changed exactly at the available date"() {
        // Spec: plan.md §3.1 rule QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE — boundary straddle on
        // lastModifiedTime > prev.availableDate; the freeze has not engaged at the available date itself
        given:
        def quiz = aNextVersionOf(aQuiz())
        quiz.setConclusionDate(QUIZ_CONCLUSION_DATE.plusHours(1))
        quiz.setLastModifiedTime(QUIZ_AVAILABLE_DATE)

        when:
        quiz.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "quiz: QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE violation — question added after the quiz became available"() {
        // Spec: plan.md §3.1 rule QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE — questions are frozen too
        given:
        def quiz = aNextVersionOf(aQuiz())
        quiz.addQuestion(aQuestion())
        quiz.setLastModifiedTime(QUIZ_AVAILABLE_DATE.plusNanos(1))

        when:
        quiz.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE
    }

    def "quiz: QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE holds when a cached question is refreshed after the available date"() {
        // Spec: plan.md §3.2 rule QUESTION_EXISTS (Quiz) — the UpdateQuestionEvent handler refreshes the
        // cached title, content and version of a QuizQuestion; only the question set is frozen
        given:
        def previous = aQuiz()
        previous.addQuestion(aQuestion())
        def quiz = aNextVersionOf(previous)
        quiz.getQuestions().first().setTitle("Refreshed title")
        quiz.setLastModifiedTime(QUIZ_AVAILABLE_DATE.plusNanos(1))

        when:
        quiz.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "quiz: QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE holds when the questions change before the available date"() {
        // Spec: plan.md §6 Quiz — UpdateQuiz may change dates or questions before the available date
        given:
        def quiz = aNextVersionOf(aQuiz())
        quiz.addQuestion(aQuestion(QUIZ_QUESTION_AGGREGATE_ID_2))
        quiz.setLastModifiedTime(QUIZ_AVAILABLE_DATE.minusNanos(1))

        when:
        quiz.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    // QUIZ_CREATION_DATE_FINAL and QUIZ_COURSE_EXECUTION_FINAL are enforced by a Java `final` field and
    // by the absence of a setter respectively; testing.md § T1 excludes both from coverage.

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
