package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.QuizAnswerDto
import java.time.LocalDateTime

class QuizAnswerDtoBuilder extends SpockTest {
    private QuizAnswerDto quizanswerDto

    static QuizAnswerDtoBuilder aQuizAnswerDto() {
        return new QuizAnswerDtoBuilder()
    }

    QuizAnswerDtoBuilder() {
        this.quizanswerDto = new QuizAnswerDto()
        // Set default values
        this.quizanswerDto.setAnswerDate(LocalDateTime.now())
        this.quizanswerDto.setCompletedDate(LocalDateTime.now())
        this.quizanswerDto.setCompleted(false)
    }

    QuizAnswerDtoBuilder withAnswerDate(LocalDateTime answerDate) {
        this.quizanswerDto.setAnswerDate(answerDate)
        return this
    }

    QuizAnswerDtoBuilder withCompletedDate(LocalDateTime completedDate) {
        this.quizanswerDto.setCompletedDate(completedDate)
        return this
    }

    QuizAnswerDtoBuilder withCompleted(Boolean completed) {
        this.quizanswerDto.setCompleted(completed)
        return this
    }

    QuizAnswerDtoBuilder withQuizAnswerStudent(Object quizAnswerStudent) {
        this.quizanswerDto.setQuizAnswerStudent(quizAnswerStudent)
        return this
    }

    QuizAnswerDtoBuilder withQuizAnswerCourseExecution(Object quizAnswerCourseExecution) {
        this.quizanswerDto.setQuizAnswerCourseExecution(quizAnswerCourseExecution)
        return this
    }

    QuizAnswerDtoBuilder withQuestionAnswers(Object questionAnswers) {
        this.quizanswerDto.setQuestionAnswers(questionAnswers)
        return this
    }

    QuizAnswerDtoBuilder withAnsweredQuiz(Object answeredQuiz) {
        this.quizanswerDto.setAnsweredQuiz(answeredQuiz)
        return this
    }

    QuizAnswerDto build() {
        return this.quizanswerDto
    }
}
