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
        this.answer.setCreationDate(LocalDateTime.now())
        this.answer.setAnswerDate(LocalDateTime.now())
        this.answer.setCompleted(false)
    }

    AnswerBuilder withCreationDate(LocalDateTime creationDate) {
        this.answer.setCreationDate(creationDate)
        return this
    }

    AnswerBuilder withAnswerDate(LocalDateTime answerDate) {
        this.answer.setAnswerDate(answerDate)
        return this
    }

    AnswerBuilder withCompleted(Boolean completed) {
        this.answer.setCompleted(completed)
        return this
    }

    AnswerBuilder withAnswerExecution(AnswerExecution answerExecution) {
        this.answer.setAnswerExecution(answerExecution)
        return this
    }

    AnswerBuilder withAnswerUser(AnswerUser answerUser) {
        this.answer.setAnswerUser(answerUser)
        return this
    }

    AnswerBuilder withAnswerQuiz(AnswerQuiz answerQuiz) {
        this.answer.setAnswerQuiz(answerQuiz)
        return this
    }

    AnswerBuilder withAnswerQuestion(List<AnswerQuestion> answerQuestion) {
        this.answer.setAnswerQuestion(answerQuestion)
        return this
    }

    Answer build() {
        return this.answer
    }
}
