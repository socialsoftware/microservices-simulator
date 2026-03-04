package pt.ulisboa.tecnico.socialsoftware.eventdriven.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class AuthorDeletedEvent extends Event {
    private String name;

    public AuthorDeletedEvent() {
        super();
    }

    public AuthorDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public AuthorDeletedEvent(Integer aggregateId, String name) {
        super(aggregateId);
        setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}