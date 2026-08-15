package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.quizanswer

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuestionAnswer
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.sagas.SagaQuizAnswer

@DataJpaTest
@Transactional
@Import(QuizAnswerIntraInvariantTest.LocalBeanConfiguration)
class QuizAnswerIntraInvariantTest extends QuizzesFull2SpockTest {

    private static SagaQuizAnswer aQuizAnswer() {
        return new SagaQuizAnswer(QUIZ_ANSWER_AGGREGATE_ID, QUIZ_AGGREGATE_ID, QUIZ_ANSWER_QUIZ_VERSION,
                USER_AGGREGATE_ID, USER_NAME, QUIZ_ANSWER_USER_VERSION, EXECUTION_AGGREGATE_ID,
                QUIZ_ANSWER_EXECUTION_VERSION, QUIZ_ANSWER_CREATION_DATE, QUIZ_ANSWER_ANSWER_DATE)
    }

    // Seeded by CreateQuizAnswer with the question's correct option key and no answer yet.
    private static QuestionAnswer anUnansweredQuestion(
            Integer questionAggregateId = QUESTION_ANSWER_QUESTION_AGGREGATE_ID) {
        return new QuestionAnswer(questionAggregateId, QUESTION_ANSWER_QUESTION_VERSION,
                QUESTION_ANSWER_CORRECT_OPTION_KEY)
    }

    private static QuestionAnswer anAnsweredQuestion(Integer optionKey, Boolean correct,
                                                    Integer questionAggregateId = QUESTION_ANSWER_QUESTION_AGGREGATE_ID) {
        def questionAnswer = anUnansweredQuestion(questionAggregateId)
        questionAnswer.setOptionSequenceChoice(QUESTION_ANSWER_OPTION_SEQUENCE_CHOICE)
        questionAnswer.setOptionKey(optionKey)
        questionAnswer.setCorrect(correct)
        questionAnswer.setTimeTaken(QUESTION_ANSWER_TIME_TAKEN)
        return questionAnswer
    }

    def "create quiz answer"() {
        // Spec: plan.md §7 QuizAnswer — CreateQuizAnswer(quizAggregateId, userAggregateId,
        // executionAggregateId); snapshots QuizAnswerQuiz / QuizAnswerStudent / QuizAnswerExecution
        // (plan.md § snapshot table)
        when:
        def quizAnswer = aQuizAnswer()
        quizAnswer.verifyInvariants()

        then:
        quizAnswer.aggregateId == QUIZ_ANSWER_AGGREGATE_ID
        quizAnswer.creationDate == QUIZ_ANSWER_CREATION_DATE
        quizAnswer.answerDate == QUIZ_ANSWER_ANSWER_DATE
        !quizAnswer.completed
        quizAnswer.quiz.quizAggregateId == QUIZ_AGGREGATE_ID
        quizAnswer.quiz.quizVersion == QUIZ_ANSWER_QUIZ_VERSION
        quizAnswer.quiz.quizAnswer.is(quizAnswer)
        quizAnswer.student.userAggregateId == USER_AGGREGATE_ID
        quizAnswer.student.userName == USER_NAME
        quizAnswer.student.userVersion == QUIZ_ANSWER_USER_VERSION
        quizAnswer.student.quizAnswer.is(quizAnswer)
        quizAnswer.execution.executionAggregateId == EXECUTION_AGGREGATE_ID
        quizAnswer.execution.executionVersion == QUIZ_ANSWER_EXECUTION_VERSION
        quizAnswer.execution.quizAnswer.is(quizAnswer)
        quizAnswer.questionAnswers.isEmpty()
        quizAnswer.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    def "quiz answer: QUESTION_ALREADY_ANSWERED violation — two answers for the same question"() {
        // Spec: plan.md §3.2 rule QUESTION_ALREADY_ANSWERED — questionAggregateIds are distinct
        given:
        def quizAnswer = aQuizAnswer()
        quizAnswer.addQuestionAnswer(anUnansweredQuestion())
        quizAnswer.addQuestionAnswer(anUnansweredQuestion())

        when:
        quizAnswer.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.QUESTION_ALREADY_ANSWERED
    }

    def "quiz answer: QUESTION_ALREADY_ANSWERED holds for answers to distinct questions"() {
        // Spec: plan.md §3.2 rule QUESTION_ALREADY_ANSWERED — the satisfying equivalence class
        given:
        def quizAnswer = aQuizAnswer()
        quizAnswer.addQuestionAnswer(anUnansweredQuestion())
        quizAnswer.addQuestionAnswer(anUnansweredQuestion(QUESTION_ANSWER_QUESTION_AGGREGATE_ID_2))

        when:
        quizAnswer.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "quiz answer: ANSWER_MATCHES_CORRECT_OPTION violation — the correct option key is marked wrong"() {
        // Spec: plan.md §3.2 rule ANSWER_MATCHES_CORRECT_OPTION —
        // correct == (optionKey == correctOptionKey)
        given:
        def quizAnswer = aQuizAnswer()
        quizAnswer.addQuestionAnswer(anAnsweredQuestion(QUESTION_ANSWER_CORRECT_OPTION_KEY, false))

        when:
        quizAnswer.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.ANSWER_MATCHES_CORRECT_OPTION
    }

    def "quiz answer: ANSWER_MATCHES_CORRECT_OPTION violation — a wrong option key is marked correct"() {
        // Spec: plan.md §3.2 rule ANSWER_MATCHES_CORRECT_OPTION — the converse violation
        given:
        def quizAnswer = aQuizAnswer()
        quizAnswer.addQuestionAnswer(anAnsweredQuestion(QUESTION_ANSWER_WRONG_OPTION_KEY, true))

        when:
        quizAnswer.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.ANSWER_MATCHES_CORRECT_OPTION
    }

    def "quiz answer: ANSWER_MATCHES_CORRECT_OPTION holds for consistently scored answers"() {
        // Spec: plan.md §3.2 rule ANSWER_MATCHES_CORRECT_OPTION — the satisfying equivalence class
        given:
        def quizAnswer = aQuizAnswer()
        quizAnswer.addQuestionAnswer(anAnsweredQuestion(QUESTION_ANSWER_CORRECT_OPTION_KEY, true))
        quizAnswer.addQuestionAnswer(anAnsweredQuestion(QUESTION_ANSWER_WRONG_OPTION_KEY, false,
                QUESTION_ANSWER_QUESTION_AGGREGATE_ID_2))

        when:
        quizAnswer.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "quiz answer: ANSWER_MATCHES_CORRECT_OPTION does not constrain an unanswered question"() {
        // Spec: plan.md §3.2 rule ANSWER_MATCHES_CORRECT_OPTION — the predicate is guarded on
        // optionKey != null, so a seeded but unanswered QuestionAnswer carries no correctness yet
        given:
        def quizAnswer = aQuizAnswer()
        quizAnswer.addQuestionAnswer(anUnansweredQuestion())

        when:
        quizAnswer.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    // QUESTION_ALREADY_ANSWERED and ANSWER_MATCHES_CORRECT_OPTION are categorical (uniqueness and
    // an equality pairing), so testing.md § Choosing Input Values excludes both from BVA straddles.
    // QUIZANSWER_FINAL_CREATION_DATE is enforced by a Java `final` field, and QUIZANSWER_FINAL_QUIZ /
    // _USER / _COURSE_EXECUTION by the absence of a setter; testing.md § T1 excludes all four.

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
