package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.quizanswer

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.sagas.SagaQuizAnswer

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class QuizAnswerTest extends QuizzesFullSpockTest {

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
}
