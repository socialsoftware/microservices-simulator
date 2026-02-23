package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class AuthorUpdatedEvent extends Event {
    private String name;
    private String bio;

    public AuthorUpdatedEvent() {
        super();
    }

    public AuthorUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public AuthorUpdatedEvent(Integer aggregateId, String name, String bio) {
        super(aggregateId);
        setName(name);
        setBio(bio);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

}