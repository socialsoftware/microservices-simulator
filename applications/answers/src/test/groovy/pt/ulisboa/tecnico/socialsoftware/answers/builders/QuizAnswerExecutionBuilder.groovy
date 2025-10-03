package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.QuizAnswerExecution
import java.time.LocalDateTime

class QuizAnswerExecutionBuilder extends SpockTest {
    private QuizAnswerExecution quizanswerexecution

    static QuizAnswerExecutionBuilder aQuizAnswerExecution() {
        return new QuizAnswerExecutionBuilder()
    }

    QuizAnswerExecutionBuilder() {
        this.quizanswerexecution = new QuizAnswerExecution()
        // Set default values
        this.quizanswerexecution.setId(1L)
        this.quizanswerexecution.setVersion(1)
        this.quizanswerexecution.setExecutionAggregateId(1)
        this.quizanswerexecution.setExecutionName("Default executionName")
        this.quizanswerexecution.setExecutionAcronym("Default executionAcronym")
        this.quizanswerexecution.setExecutionAcademicTerm("Default executionAcademicTerm")
    }

    QuizAnswerExecutionBuilder withExecutionAggregateId(Integer executionAggregateId) {
        this.quizanswerexecution.setExecutionAggregateId(executionAggregateId)
        return this
    }

    QuizAnswerExecutionBuilder withExecutionName(String executionName) {
        this.quizanswerexecution.setExecutionName(executionName)
        return this
    }

    QuizAnswerExecutionBuilder withExecutionAcronym(String executionAcronym) {
        this.quizanswerexecution.setExecutionAcronym(executionAcronym)
        return this
    }

    QuizAnswerExecutionBuilder withExecutionAcademicTerm(String executionAcademicTerm) {
        this.quizanswerexecution.setExecutionAcademicTerm(executionAcademicTerm)
        return this
    }

    QuizAnswerExecution build() {
        return this.quizanswerexecution
    }
}
