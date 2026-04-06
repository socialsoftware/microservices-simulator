package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class AnswerExecutionUpdatedEvent extends Event {
    @Column(name = "answer_execution_updated_event_execution_aggregate_id")
    private Integer executionAggregateId;
    @Column(name = "answer_execution_updated_event_execution_version")
    private Integer executionVersion;

    public AnswerExecutionUpdatedEvent() {
        super();
    }

    public AnswerExecutionUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
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