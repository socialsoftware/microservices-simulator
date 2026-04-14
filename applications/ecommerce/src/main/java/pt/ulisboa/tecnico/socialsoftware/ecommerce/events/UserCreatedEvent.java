package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class UserCreatedEvent extends Event {
    private String username;
    private String email;

    public UserCreatedEvent() {
        super();
    }

    public UserCreatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public UserCreatedEvent(Integer aggregateId, String username, String email) {
        super(aggregateId);
        setUsername(username);
        setEmail(email);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}