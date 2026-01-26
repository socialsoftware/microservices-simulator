package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TopicUpdatedEvent extends Event {
    private String name;

    public TopicUpdatedEvent() {
        super();
    }

    public TopicUpdatedEvent(Integer aggregateId, String name) {
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