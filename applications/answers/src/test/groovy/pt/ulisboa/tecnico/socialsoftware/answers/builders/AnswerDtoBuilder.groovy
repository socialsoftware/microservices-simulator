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
        this.answerDto.setAnswerDate(LocalDateTime.now())
        this.answerDto.setCompletedDate(LocalDateTime.now())
        this.answerDto.setCompleted(false)
    }

    AnswerDtoBuilder withAnswerDate(LocalDateTime answerDate) {
        this.answerDto.setAnswerDate(answerDate)
        return this
    }

    AnswerDtoBuilder withCompletedDate(LocalDateTime completedDate) {
        this.answerDto.setCompletedDate(completedDate)
        return this
    }

    AnswerDtoBuilder withCompleted(Boolean completed) {
        this.answerDto.setCompleted(completed)
        return this
    }

    AnswerDtoBuilder withQuizAnswerStudent(QuizAnswerStudent quizAnswerStudent) {
        this.answerDto.setQuizAnswerStudent(quizAnswerStudent)
        return this
    }

    AnswerDtoBuilder withQuizAnswerExecution(QuizAnswerExecution quizAnswerExecution) {
        this.answerDto.setQuizAnswerExecution(quizAnswerExecution)
        return this
    }

    AnswerDtoBuilder withQuestionAnswers(Set<QuestionAnswer> questionAnswers) {
        this.answerDto.setQuestionAnswers(questionAnswers)
        return this
    }

    AnswerDtoBuilder withAnsweredQuiz(AnsweredQuiz answeredQuiz) {
        this.answerDto.setAnsweredQuiz(answeredQuiz)
        return this
    }

    AnswerDto build() {
        return this.answerDto
    }
}
