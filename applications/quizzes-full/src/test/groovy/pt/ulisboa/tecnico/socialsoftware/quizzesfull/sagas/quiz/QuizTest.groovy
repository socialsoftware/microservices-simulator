package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.quiz

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizExecution
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizQuestion
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizType
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.sagas.SagaQuiz
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionDto

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class QuizTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "create quiz"() {
        given:
        def executionDto = new ExecutionDto()
        executionDto.setAggregateId(100)
        executionDto.setVersion(1L)
        def quizExecution = new QuizExecution(executionDto)

        def questionDto = new QuestionDto()
        questionDto.setAggregateId(200)
        questionDto.setVersion(1L)
        questionDto.setTitle("What is 2+2?")
        questionDto.setContent("Choose the correct answer.")
        def quizQuestion = new QuizQuestion(questionDto)

        def available = LocalDateTime.now().plusDays(1)
        def conclusion = LocalDateTime.now().plusDays(2)
        def results = LocalDateTime.now().plusDays(3)

        when:
        def quiz = new SagaQuiz(1, "Sample Quiz", available, conclusion, results,
                QuizType.TEST, quizExecution, [quizQuestion] as Set)

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
}
