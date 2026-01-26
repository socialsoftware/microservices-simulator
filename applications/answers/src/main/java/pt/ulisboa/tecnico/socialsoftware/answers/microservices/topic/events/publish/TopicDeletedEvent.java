package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TopicDeletedEvent extends Event {

    public TopicDeletedEvent() {
        super();
    }

    public TopicDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

}