package pt.ulisboa.tecnico.socialsoftware.teastore.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CategoryDeletedEvent extends Event {
    private String name;

    public CategoryDeletedEvent() {
        super();
    }

    public CategoryDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public CategoryDeletedEvent(Integer aggregateId, String name) {
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