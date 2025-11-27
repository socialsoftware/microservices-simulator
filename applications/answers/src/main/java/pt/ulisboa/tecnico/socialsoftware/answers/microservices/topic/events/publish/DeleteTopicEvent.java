package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class DeleteTopicEvent extends Event {

    public DeleteTopicEvent() {
    }

    public DeleteTopicEvent(Integer aggregateId) {
        super(aggregateId);
    }

}