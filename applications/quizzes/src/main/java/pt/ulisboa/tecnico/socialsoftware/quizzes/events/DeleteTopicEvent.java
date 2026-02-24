package pt.ulisboa.tecnico.socialsoftware.quizzes.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class DeleteTopicEvent extends Event {
    public DeleteTopicEvent() {
        super();
    }
    public DeleteTopicEvent(Integer topicAggregateId) {
        super(topicAggregateId);
    }
}
