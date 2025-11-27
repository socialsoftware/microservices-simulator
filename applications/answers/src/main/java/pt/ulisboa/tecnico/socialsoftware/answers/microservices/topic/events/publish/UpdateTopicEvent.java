package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class UpdateTopicEvent extends Event {
    private String topicName;

    public UpdateTopicEvent() {
    }

    public UpdateTopicEvent(Integer aggregateId, String topicName) {
        super(aggregateId);
        setTopicName(topicName);
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

}