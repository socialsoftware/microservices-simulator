package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.events.publish;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

import jakarta.persistence.Entity;

@Entity
public class DeleteUserEvent extends Event {
    public DeleteUserEvent() {
        super();
    }

    public DeleteUserEvent(Integer userAggregateId) {
        super(userAggregateId);
    }
}




