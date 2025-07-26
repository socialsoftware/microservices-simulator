package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class DeleteUserEvent extends Event {
    public DeleteUserEvent() {
        super();
    }

    public DeleteUserEvent(Integer userAggregateId) {
        super(userAggregateId);
    }
}




