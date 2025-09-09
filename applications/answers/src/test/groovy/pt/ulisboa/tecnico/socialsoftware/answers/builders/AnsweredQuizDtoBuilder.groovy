package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.AnsweredQuizDto
import java.time.LocalDateTime

class AnsweredQuizDtoBuilder extends SpockTest {
    private AnsweredQuizDto answeredquizDto

    static AnsweredQuizDtoBuilder aAnsweredQuizDto() {
        return new AnsweredQuizDtoBuilder()
    }

    AnsweredQuizDtoBuilder() {
        this.answeredquizDto = new AnsweredQuizDto()
        // Set default values
        this.answeredquizDto.setQuizAggregateId(1)
        this.answeredquizDto.setQuizTitle("Default quizTitle")
        this.answeredquizDto.setQuizType("Default quizType")
        this.answeredquizDto.setAvailableDate(LocalDateTime.now())
        this.answeredquizDto.setConclusionDate(LocalDateTime.now())
        this.answeredquizDto.setNumberOfQuestions(1)
    }

    AnsweredQuizDtoBuilder withQuizAggregateId(Integer quizAggregateId) {
        this.answeredquizDto.setQuizAggregateId(quizAggregateId)
        return this
    }

    AnsweredQuizDtoBuilder withQuizTitle(String quizTitle) {
        this.answeredquizDto.setQuizTitle(quizTitle)
        return this
    }

    AnsweredQuizDtoBuilder withQuizType(String quizType) {
        this.answeredquizDto.setQuizType(quizType)
        return this
    }

    AnsweredQuizDtoBuilder withAvailableDate(LocalDateTime availableDate) {
        this.answeredquizDto.setAvailableDate(availableDate)
        return this
    }

    AnsweredQuizDtoBuilder withConclusionDate(LocalDateTime conclusionDate) {
        this.answeredquizDto.setConclusionDate(conclusionDate)
        return this
    }

    AnsweredQuizDtoBuilder withNumberOfQuestions(Integer numberOfQuestions) {
        this.answeredquizDto.setNumberOfQuestions(numberOfQuestions)
        return this
    }

    AnsweredQuizDto build() {
        return this.answeredquizDto
    }
}
