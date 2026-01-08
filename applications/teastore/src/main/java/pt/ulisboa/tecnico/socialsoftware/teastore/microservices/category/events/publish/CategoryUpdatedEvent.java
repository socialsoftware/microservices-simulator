package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CategoryUpdatedEvent extends Event {
    private String name;
    private String description;

    public CategoryUpdatedEvent() {
    }

    public CategoryUpdatedEvent(Integer aggregateId, String name, String description) {
        super(aggregateId);
        setName(name);
        setDescription(description);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}