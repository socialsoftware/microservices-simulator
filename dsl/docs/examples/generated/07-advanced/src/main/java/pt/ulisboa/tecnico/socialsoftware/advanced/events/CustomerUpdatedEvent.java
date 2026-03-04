package pt.ulisboa.tecnico.socialsoftware.advanced.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CustomerUpdatedEvent extends Event {
    private String name;
    private String email;
    private Boolean active;

    public CustomerUpdatedEvent() {
        super();
    }

    public CustomerUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public CustomerUpdatedEvent(Integer aggregateId, String name, String email, Boolean active) {
        super(aggregateId);
        setName(name);
        setEmail(email);
        setActive(active);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

}