package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class AnswerExecutionDeletedEvent extends Event {
    @Column(name = "answer_execution_deleted_event_execution_aggregate_id")
    private Integer executionAggregateId;

    public AnswerExecutionDeletedEvent() {
        super();
    }

    public AnswerExecutionDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public AnswerExecutionDeletedEvent(Integer aggregateId, Integer executionAggregateId) {
        super(aggregateId);
        setExecutionAggregateId(executionAggregateId);
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

}