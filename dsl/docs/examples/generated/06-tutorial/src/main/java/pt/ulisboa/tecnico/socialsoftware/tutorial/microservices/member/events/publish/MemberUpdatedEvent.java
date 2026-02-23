package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class MemberUpdatedEvent extends Event {
    private String name;
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