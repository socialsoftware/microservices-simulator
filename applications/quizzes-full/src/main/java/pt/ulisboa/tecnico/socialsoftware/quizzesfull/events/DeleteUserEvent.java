package pt.ulisboa.tecnico.socialsoftware.quizzesfull.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class DeleteUserEvent extends Event {
    public DeleteUserEvent() {
        super();
    }

    public DeleteUserEvent(Integer userAggregateId) {
        super(userAggregateId);
    }
}
