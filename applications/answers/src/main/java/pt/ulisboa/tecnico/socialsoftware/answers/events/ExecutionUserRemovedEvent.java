package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class ExecutionUserRemovedEvent extends Event {
    @Column(name = "execution_user_removed_event_user_aggregate_id")
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