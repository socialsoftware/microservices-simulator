package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.Answer
import java.time.LocalDateTime

class AnswerBuilder extends SpockTest {
    private Answer answer

    static AnswerBuilder aAnswer() {
        return new AnswerBuilder()
    }

    AnswerBuilder() {
        this.answer = new Answer()
        // Set default values
        this.answer.setId(1L)
        this.answer.setVersion(1)
        this.answer.setAnswerDate(LocalDateTime.now())
        this.answer.setCompletedDate(LocalDateTime.now())
        this.answer.setCompleted(false)
    }

    AnswerBuilder withAnswerDate(LocalDateTime answerDate) {
        this.answer.setAnswerDate(answerDate)
        return this
    }

    AnswerBuilder withCompletedDate(LocalDateTime completedDate) {
        this.answer.setCompletedDate(completedDate)
        return this
    }

    AnswerBuilder withCompleted(Boolean completed) {
        this.answer.setCompleted(completed)
        return this
    }

    AnswerBuilder withQuizAnswerStudent(QuizAnswerStudent quizAnswerStudent) {
        this.answer.setQuizAnswerStudent(quizAnswerStudent)
        return this
    }

    AnswerBuilder withQuizAnswerExecution(QuizAnswerExecution quizAnswerExecution) {
        this.answer.setQuizAnswerExecution(quizAnswerExecution)
        return this
    }

    AnswerBuilder withQuestionAnswers(Set<QuestionAnswer> questionAnswers) {
        this.answer.setQuestionAnswers(questionAnswers)
        return this
    }

    AnswerBuilder withAnsweredQuiz(AnsweredQuiz answeredQuiz) {
        this.answer.setAnsweredQuiz(answeredQuiz)
        return this
    }

    Answer build() {
        return this.answer
    }
}
