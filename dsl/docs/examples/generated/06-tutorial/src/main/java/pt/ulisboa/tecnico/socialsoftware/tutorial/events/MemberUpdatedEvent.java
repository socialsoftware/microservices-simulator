package pt.ulisboa.tecnico.socialsoftware.tutorial.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class MemberUpdatedEvent extends Event {
    @Column(name = "member_updated_event_name")
    private String name;
    @Column(name = "member_updated_event_email")
    private String email;

    public MemberUpdatedEvent() {
        super();
    }

    public MemberUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public MemberUpdatedEvent(Integer aggregateId, String name, String email) {
        super(aggregateId);
        setName(name);
        setEmail(email);
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

}