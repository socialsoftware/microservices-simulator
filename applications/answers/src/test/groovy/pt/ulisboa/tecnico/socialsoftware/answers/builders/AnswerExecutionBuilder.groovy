package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.AnswerExecution
import java.time.LocalDateTime

class AnswerExecutionBuilder extends SpockTest {
    private AnswerExecution answerexecution

    static AnswerExecutionBuilder aAnswerExecution() {
        return new AnswerExecutionBuilder()
    }

    AnswerExecutionBuilder() {
        this.answerexecution = new AnswerExecution()
        // Set default values
        this.answerexecution.setId(1L)
        this.answerexecution.setVersion(1)
        this.answerexecution.setExecutionAggregateId(1)
        this.answerexecution.setExecutionVersion(1)
    }

    AnswerExecutionBuilder withId(Long id) {
        this.answerexecution.setId(id)
        return this
    }

    AnswerExecutionBuilder withExecutionAggregateId(Integer executionAggregateId) {
        this.answerexecution.setExecutionAggregateId(executionAggregateId)
        return this
    }

    AnswerExecutionBuilder withExecutionVersion(Integer executionVersion) {
        this.answerexecution.setExecutionVersion(executionVersion)
        return this
    }

    AnswerExecution build() {
        return this.answerexecution
    }
}
