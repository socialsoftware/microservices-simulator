package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class QuizExecutionDeletedEvent extends Event {
    @Column(name = "quiz_execution_deleted_event_execution_aggregate_id")
    private Integer executionAggregateId;

    public QuizExecutionDeletedEvent() {
        super();
    }

    public QuizExecutionDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public QuizExecutionDeletedEvent(Integer aggregateId, Integer executionAggregateId) {
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