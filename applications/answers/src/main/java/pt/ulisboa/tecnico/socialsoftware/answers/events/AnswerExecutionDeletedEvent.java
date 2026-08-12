package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class AnswerExecutionDeletedEvent extends Event {
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