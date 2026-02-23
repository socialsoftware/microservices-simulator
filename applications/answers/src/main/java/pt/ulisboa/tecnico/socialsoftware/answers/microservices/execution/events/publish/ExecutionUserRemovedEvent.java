package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ExecutionUserRemovedEvent extends Event {
    private Integer userAggregateId;

    public ExecutionUserRemovedEvent() {
        super();
    }

    public ExecutionUserRemovedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ExecutionUserRemovedEvent(Integer aggregateId, Integer userAggregateId) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

}