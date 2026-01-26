package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class UserUpdatedEvent extends Event {
    private String name;
    private String username;
    private Boolean active;

    public UserUpdatedEvent() {
        super();
    }

    public UserUpdatedEvent(Integer aggregateId, String name, String username, Boolean active) {
        super(aggregateId);
        setName(name);
        setUsername(username);
        setActive(active);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

}