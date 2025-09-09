package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.QuizAnswer
import java.time.LocalDateTime

class QuizAnswerBuilder extends SpockTest {
    private QuizAnswer quizanswer

    static QuizAnswerBuilder aQuizAnswer() {
        return new QuizAnswerBuilder()
    }

    QuizAnswerBuilder() {
        this.quizanswer = new QuizAnswer()
        // Set default values
        this.quizanswer.setId(1L)
        this.quizanswer.setVersion(1)
        this.quizanswer.setAnswerDate(LocalDateTime.now())
        this.quizanswer.setCompletedDate(LocalDateTime.now())
        this.quizanswer.setCompleted(false)
    }

    QuizAnswerBuilder withAnswerDate(LocalDateTime answerDate) {
        this.quizanswer.setAnswerDate(answerDate)
        return this
    }

    QuizAnswerBuilder withCompletedDate(LocalDateTime completedDate) {
        this.quizanswer.setCompletedDate(completedDate)
        return this
    }

    QuizAnswerBuilder withCompleted(Boolean completed) {
        this.quizanswer.setCompleted(completed)
        return this
    }

    QuizAnswerBuilder withQuizAnswerStudent(Object quizAnswerStudent) {
        this.quizanswer.setQuizAnswerStudent(quizAnswerStudent)
        return this
    }

    QuizAnswerBuilder withQuizAnswerCourseExecution(Object quizAnswerCourseExecution) {
        this.quizanswer.setQuizAnswerCourseExecution(quizAnswerCourseExecution)
        return this
    }

    QuizAnswerBuilder withQuestionAnswers(Object questionAnswers) {
        this.quizanswer.setQuestionAnswers(questionAnswers)
        return this
    }

    QuizAnswerBuilder withAnsweredQuiz(Object answeredQuiz) {
        this.quizanswer.setAnsweredQuiz(answeredQuiz)
        return this
    }

    QuizAnswer build() {
        return this.quizanswer
    }
}
