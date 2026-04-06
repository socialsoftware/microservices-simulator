package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class ExecutionUserDeletedEvent extends Event {
    @Column(name = "execution_user_deleted_event_user_aggregate_id")
    private Integer userAggregateId;

    public ExecutionUserDeletedEvent() {
        super();
    }

    public ExecutionUserDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ExecutionUserDeletedEvent(Integer aggregateId, Integer userAggregateId) {
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