package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class DeleteExecutionEvent extends Event {
    private Integer executionAggregateId;
    private String acronym;

    public DeleteExecutionEvent() {
    }

    public DeleteExecutionEvent(Integer aggregateId, Integer executionAggregateId, String acronym) {
        super(aggregateId);
        setExecutionAggregateId(executionAggregateId);
        setAcronym(acronym);
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

}