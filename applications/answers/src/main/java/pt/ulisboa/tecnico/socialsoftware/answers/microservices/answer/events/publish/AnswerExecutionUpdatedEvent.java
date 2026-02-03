package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class AnswerExecutionUpdatedEvent extends Event {
    private Integer executionAggregateId;
    private Integer executionVersion;

    public AnswerExecutionUpdatedEvent() {
        super();
    }

    public AnswerExecutionUpdatedEvent(Integer aggregateId, Integer executionAggregateId, Integer executionVersion) {
        super(aggregateId);
        setExecutionAggregateId(executionAggregateId);
        setExecutionVersion(executionVersion);
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Integer getExecutionVersion() {
        return executionVersion;
    }

    public void setExecutionVersion(Integer executionVersion) {
        this.executionVersion = executionVersion;
    }

}