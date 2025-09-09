package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.QuestionAnswer
import java.time.LocalDateTime

class QuestionAnswerBuilder extends SpockTest {
    private QuestionAnswer questionanswer

    static QuestionAnswerBuilder aQuestionAnswer() {
        return new QuestionAnswerBuilder()
    }

    QuestionAnswerBuilder() {
        this.questionanswer = new QuestionAnswer()
        // Set default values
        this.questionanswer.setId(1L)
        this.questionanswer.setVersion(1)
        this.questionanswer.setQuestionId(1)
        this.questionanswer.setAnswer("Default answer")
        this.questionanswer.setOption("Default option")
        this.questionanswer.setAnswerDate(LocalDateTime.now())
    }

    QuestionAnswerBuilder withQuestionId(Integer questionId) {
        this.questionanswer.setQuestionId(questionId)
        return this
    }

    QuestionAnswerBuilder withAnswer(String answer) {
        this.questionanswer.setAnswer(answer)
        return this
    }

    QuestionAnswerBuilder withOption(String option) {
        this.questionanswer.setOption(option)
        return this
    }

    QuestionAnswerBuilder withAnswerDate(LocalDateTime answerDate) {
        this.questionanswer.setAnswerDate(answerDate)
        return this
    }

    QuestionAnswer build() {
        return this.questionanswer
    }
}
