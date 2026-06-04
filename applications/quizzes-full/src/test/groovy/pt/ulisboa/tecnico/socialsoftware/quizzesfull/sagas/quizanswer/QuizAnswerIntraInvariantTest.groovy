package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.quizanswer

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuestionAnswer
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.sagas.SagaQuizAnswer

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.QUESTION_ALREADY_ANSWERED

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class QuizAnswerIntraInvariantTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "create quiz answer"() {
        when:
        def quizAnswer = new SagaQuizAnswer(1, 100, 1L, 200, 1L, "Alice", "alice", 300, 1L)

        then:
        quizAnswer.quizAggregateId == 100
        quizAnswer.quizVersion == 1L
        quizAnswer.userAggregateId == 200
        quizAnswer.userVersion == 1L
        quizAnswer.userName == "Alice"
        quizAnswer.userUsername == "alice"
        quizAnswer.executionAggregateId == 300
        quizAnswer.executionVersion == 1L
        quizAnswer.completed == false
        quizAnswer.creationDate != null
        quizAnswer.answerDate != null
        quizAnswer.questionAnswers.isEmpty()
    }

    def "QUESTION_ALREADY_ANSWERED: distinct questionIds pass invariant"() {
        given: "a quiz answer with two question answers for different questions"
        def quizAnswer = new SagaQuizAnswer(1, 100, 1L, 200, 1L, "Alice", "alice", 300, 1L)
        quizAnswer.addQuestionAnswer(new QuestionAnswer(10, 1L, 1, 42, true, 30))
        quizAnswer.addQuestionAnswer(new QuestionAnswer(20, 1L, 2, 43, false, 45))

        when:
        quizAnswer.verifyInvariants()

        then:
        noExceptionThrown()
    }

    def "QUESTION_ALREADY_ANSWERED: duplicate questionId in QuestionAnswer collection violates invariant"() {
        given: "a quiz answer with two QuestionAnswers sharing the same questionAggregateId"
        def quizAnswer = new SagaQuizAnswer(1, 100, 1L, 200, 1L, "Alice", "alice", 300, 1L)
        quizAnswer.addQuestionAnswer(new QuestionAnswer(10, 1L, 1, 42, true, 30))
        quizAnswer.addQuestionAnswer(new QuestionAnswer(10, 1L, 2, 43, false, 45))

        when:
        quizAnswer.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == QUESTION_ALREADY_ANSWERED
    }
}
