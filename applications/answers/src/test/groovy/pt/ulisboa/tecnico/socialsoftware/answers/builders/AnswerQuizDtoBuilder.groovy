package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.AnswerQuizDto
import java.time.LocalDateTime

class AnswerQuizDtoBuilder extends SpockTest {
    private AnswerQuizDto answerquizDto

    static AnswerQuizDtoBuilder aAnswerQuizDto() {
        return new AnswerQuizDtoBuilder()
    }

    AnswerQuizDtoBuilder() {
        this.answerquizDto = new AnswerQuizDto()
        // Set default values
        this.answerquizDto.setId(1L)
        this.answerquizDto.setQuizAggregateId(1)
        this.answerquizDto.setQuizVersion(1)
    }

    AnswerQuizDtoBuilder withId(Long id) {
        this.answerquizDto.setId(id)
        return this
    }

    AnswerQuizDtoBuilder withQuizAggregateId(Integer quizAggregateId) {
        this.answerquizDto.setQuizAggregateId(quizAggregateId)
        return this
    }

    AnswerQuizDtoBuilder withQuizVersion(Integer quizVersion) {
        this.answerquizDto.setQuizVersion(quizVersion)
        return this
    }

    AnswerQuizDtoBuilder withQuizQuestionsAggregateIds(List<Integer> quizQuestionsAggregateIds) {
        this.answerquizDto.setQuizQuestionsAggregateIds(quizQuestionsAggregateIds)
        return this
    }

    AnswerQuizDto build() {
        return this.answerquizDto
    }
}
