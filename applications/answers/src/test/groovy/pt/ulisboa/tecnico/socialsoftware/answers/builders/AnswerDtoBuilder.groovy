package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.AnswerDto
import java.time.LocalDateTime

class AnswerDtoBuilder extends SpockTest {
    private AnswerDto answerDto

    static AnswerDtoBuilder aAnswerDto() {
        return new AnswerDtoBuilder()
    }

    AnswerDtoBuilder() {
        this.answerDto = new AnswerDto()
        // Set default values
        this.answerDto.setCreationDate(LocalDateTime.now())
        this.answerDto.setAnswerDate(LocalDateTime.now())
        this.answerDto.setCompleted(false)
    }

    AnswerDtoBuilder withCreationDate(LocalDateTime creationDate) {
        this.answerDto.setCreationDate(creationDate)
        return this
    }

    AnswerDtoBuilder withAnswerDate(LocalDateTime answerDate) {
        this.answerDto.setAnswerDate(answerDate)
        return this
    }

    AnswerDtoBuilder withCompleted(Boolean completed) {
        this.answerDto.setCompleted(completed)
        return this
    }

    AnswerDtoBuilder withAnswerExecution(AnswerExecution answerExecution) {
        this.answerDto.setAnswerExecution(answerExecution)
        return this
    }

    AnswerDtoBuilder withAnswerUser(AnswerUser answerUser) {
        this.answerDto.setAnswerUser(answerUser)
        return this
    }

    AnswerDtoBuilder withAnswerQuiz(AnswerQuiz answerQuiz) {
        this.answerDto.setAnswerQuiz(answerQuiz)
        return this
    }

    AnswerDtoBuilder withAnswerQuestion(Set<AnswerQuestion> answerQuestion) {
        this.answerDto.setAnswerQuestion(answerQuestion)
        return this
    }

    AnswerDto build() {
        return this.answerDto
    }
}
