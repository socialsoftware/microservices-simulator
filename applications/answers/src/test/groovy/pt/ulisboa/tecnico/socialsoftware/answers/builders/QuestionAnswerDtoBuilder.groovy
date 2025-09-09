package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.QuestionAnswerDto
import java.time.LocalDateTime

class QuestionAnswerDtoBuilder extends SpockTest {
    private QuestionAnswerDto questionanswerDto

    static QuestionAnswerDtoBuilder aQuestionAnswerDto() {
        return new QuestionAnswerDtoBuilder()
    }

    QuestionAnswerDtoBuilder() {
        this.questionanswerDto = new QuestionAnswerDto()
        // Set default values
        this.questionanswerDto.setQuestionId(1)
        this.questionanswerDto.setAnswer("Default answer")
        this.questionanswerDto.setOption("Default option")
        this.questionanswerDto.setAnswerDate(LocalDateTime.now())
    }

    QuestionAnswerDtoBuilder withQuestionId(Integer questionId) {
        this.questionanswerDto.setQuestionId(questionId)
        return this
    }

    QuestionAnswerDtoBuilder withAnswer(String answer) {
        this.questionanswerDto.setAnswer(answer)
        return this
    }

    QuestionAnswerDtoBuilder withOption(String option) {
        this.questionanswerDto.setOption(option)
        return this
    }

    QuestionAnswerDtoBuilder withAnswerDate(LocalDateTime answerDate) {
        this.questionanswerDto.setAnswerDate(answerDate)
        return this
    }

    QuestionAnswerDto build() {
        return this.questionanswerDto
    }
}
