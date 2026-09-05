package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class DeleteUserEvent extends Event {
    private Integer userAggregateId;

    protected DeleteUserEvent() {}

    public DeleteUserEvent(Integer userAggregateId) {
        super(userAggregateId);
        this.userAggregateId = userAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }
}
