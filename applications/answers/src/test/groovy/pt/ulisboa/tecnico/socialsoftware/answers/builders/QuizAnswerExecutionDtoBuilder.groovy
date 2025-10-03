package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.QuizAnswerExecutionDto
import java.time.LocalDateTime

class QuizAnswerExecutionDtoBuilder extends SpockTest {
    private QuizAnswerExecutionDto quizanswerexecutionDto

    static QuizAnswerExecutionDtoBuilder aQuizAnswerExecutionDto() {
        return new QuizAnswerExecutionDtoBuilder()
    }

    QuizAnswerExecutionDtoBuilder() {
        this.quizanswerexecutionDto = new QuizAnswerExecutionDto()
        // Set default values
        this.quizanswerexecutionDto.setExecutionAggregateId(1)
        this.quizanswerexecutionDto.setExecutionName("Default executionName")
        this.quizanswerexecutionDto.setExecutionAcronym("Default executionAcronym")
        this.quizanswerexecutionDto.setExecutionAcademicTerm("Default executionAcademicTerm")
    }

    QuizAnswerExecutionDtoBuilder withExecutionAggregateId(Integer executionAggregateId) {
        this.quizanswerexecutionDto.setExecutionAggregateId(executionAggregateId)
        return this
    }

    QuizAnswerExecutionDtoBuilder withExecutionName(String executionName) {
        this.quizanswerexecutionDto.setExecutionName(executionName)
        return this
    }

    QuizAnswerExecutionDtoBuilder withExecutionAcronym(String executionAcronym) {
        this.quizanswerexecutionDto.setExecutionAcronym(executionAcronym)
        return this
    }

    QuizAnswerExecutionDtoBuilder withExecutionAcademicTerm(String executionAcademicTerm) {
        this.quizanswerexecutionDto.setExecutionAcademicTerm(executionAcademicTerm)
        return this
    }

    QuizAnswerExecutionDto build() {
        return this.quizanswerexecutionDto
    }
}
