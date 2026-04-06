package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class TopicUpdatedEvent extends Event {
    @Column(name = "topic_updated_event_name")
    private String name;

    public TopicUpdatedEvent() {
        super();
    }

    public TopicUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
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