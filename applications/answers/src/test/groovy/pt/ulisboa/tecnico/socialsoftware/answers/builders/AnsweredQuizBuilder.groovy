package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.AnsweredQuiz
import java.time.LocalDateTime

class AnsweredQuizBuilder extends SpockTest {
    private AnsweredQuiz answeredquiz

    static AnsweredQuizBuilder aAnsweredQuiz() {
        return new AnsweredQuizBuilder()
    }

    AnsweredQuizBuilder() {
        this.answeredquiz = new AnsweredQuiz()
        // Set default values
        this.answeredquiz.setId(1L)
        this.answeredquiz.setVersion(1)
        this.answeredquiz.setQuizAggregateId(1)
        this.answeredquiz.setQuizTitle("Default quizTitle")
        this.answeredquiz.setQuizType("Default quizType")
        this.answeredquiz.setAvailableDate(LocalDateTime.now())
        this.answeredquiz.setConclusionDate(LocalDateTime.now())
        this.answeredquiz.setNumberOfQuestions(1)
    }

    AnsweredQuizBuilder withQuizAggregateId(Integer quizAggregateId) {
        this.answeredquiz.setQuizAggregateId(quizAggregateId)
        return this
    }

    AnsweredQuizBuilder withQuizTitle(String quizTitle) {
        this.answeredquiz.setQuizTitle(quizTitle)
        return this
    }

    AnsweredQuizBuilder withQuizType(String quizType) {
        this.answeredquiz.setQuizType(quizType)
        return this
    }

    AnsweredQuizBuilder withAvailableDate(LocalDateTime availableDate) {
        this.answeredquiz.setAvailableDate(availableDate)
        return this
    }

    AnsweredQuizBuilder withConclusionDate(LocalDateTime conclusionDate) {
        this.answeredquiz.setConclusionDate(conclusionDate)
        return this
    }

    AnsweredQuizBuilder withNumberOfQuestions(Integer numberOfQuestions) {
        this.answeredquiz.setNumberOfQuestions(numberOfQuestions)
        return this
    }

    AnsweredQuiz build() {
        return this.answeredquiz
    }
}
