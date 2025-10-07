package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.AnswerQuiz
import java.time.LocalDateTime

class AnswerQuizBuilder extends SpockTest {
    private AnswerQuiz answerquiz

    static AnswerQuizBuilder aAnswerQuiz() {
        return new AnswerQuizBuilder()
    }

    AnswerQuizBuilder() {
        this.answerquiz = new AnswerQuiz()
        // Set default values
        this.answerquiz.setId(1L)
        this.answerquiz.setVersion(1)
        this.answerquiz.setQuizAggregateId(1)
        this.answerquiz.setQuizVersion(1)
    }

    AnswerQuizBuilder withId(Long id) {
        this.answerquiz.setId(id)
        return this
    }

    AnswerQuizBuilder withQuizAggregateId(Integer quizAggregateId) {
        this.answerquiz.setQuizAggregateId(quizAggregateId)
        return this
    }

    AnswerQuizBuilder withQuizVersion(Integer quizVersion) {
        this.answerquiz.setQuizVersion(quizVersion)
        return this
    }

    AnswerQuizBuilder withQuizQuestionsAggregateIds(Set<Object> quizQuestionsAggregateIds) {
        this.answerquiz.setQuizQuestionsAggregateIds(quizQuestionsAggregateIds)
        return this
    }

    AnswerQuiz build() {
        return this.answerquiz
    }
}
