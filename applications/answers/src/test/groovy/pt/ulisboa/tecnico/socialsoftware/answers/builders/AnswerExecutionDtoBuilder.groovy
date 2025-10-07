package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.AnswerExecutionDto
import java.time.LocalDateTime

class AnswerExecutionDtoBuilder extends SpockTest {
    private AnswerExecutionDto answerexecutionDto

    static AnswerExecutionDtoBuilder aAnswerExecutionDto() {
        return new AnswerExecutionDtoBuilder()
    }

    AnswerExecutionDtoBuilder() {
        this.answerexecutionDto = new AnswerExecutionDto()
        // Set default values
        this.answerexecutionDto.setId(1L)
        this.answerexecutionDto.setExecutionAggregateId(1)
        this.answerexecutionDto.setExecutionVersion(1)
    }

    AnswerExecutionDtoBuilder withId(Long id) {
        this.answerexecutionDto.setId(id)
        return this
    }

    AnswerExecutionDtoBuilder withExecutionAggregateId(Integer executionAggregateId) {
        this.answerexecutionDto.setExecutionAggregateId(executionAggregateId)
        return this
    }

    AnswerExecutionDtoBuilder withExecutionVersion(Integer executionVersion) {
        this.answerexecutionDto.setExecutionVersion(executionVersion)
        return this
    }

    AnswerExecutionDto build() {
        return this.answerexecutionDto
    }
}
