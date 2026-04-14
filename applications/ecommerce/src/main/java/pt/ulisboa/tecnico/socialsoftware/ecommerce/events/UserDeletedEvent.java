package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class UserDeletedEvent extends Event {
    private String username;

    public UserDeletedEvent() {
        super();
    }

    public UserDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public UserDeletedEvent(Integer aggregateId, String username) {
        super(aggregateId);
        setUsername(username);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}