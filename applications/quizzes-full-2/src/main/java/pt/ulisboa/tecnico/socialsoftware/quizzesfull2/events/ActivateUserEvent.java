package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class ActivateUserEvent extends Event {
    private Integer userAggregateId;
    private Boolean active;

    protected ActivateUserEvent() {}

    public ActivateUserEvent(Integer userAggregateId, Boolean active) {
        super(userAggregateId);
        this.userAggregateId = userAggregateId;
        this.active = active;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public Boolean isActive() {
        return active;
    }
}
