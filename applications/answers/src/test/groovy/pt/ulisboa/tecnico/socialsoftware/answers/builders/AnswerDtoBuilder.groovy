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

    AnswerDtoBuilder withQuizAnswerStudent(Object quizAnswerStudent) {
        this.answerDto.setQuizAnswerStudent(quizAnswerStudent)
        return this
    }

    AnswerDtoBuilder withQuizAnswerCourseExecution(Object quizAnswerCourseExecution) {
        this.answerDto.setQuizAnswerCourseExecution(quizAnswerCourseExecution)
        return this
    }

    AnswerDtoBuilder withQuestionAnswers(Object questionAnswers) {
        this.answerDto.setQuestionAnswers(questionAnswers)
        return this
    }

    AnswerDtoBuilder withAnsweredQuiz(Object answeredQuiz) {
        this.answerDto.setAnsweredQuiz(answeredQuiz)
        return this
    }

    AnswerDto build() {
        return this.answerDto
    }
}
