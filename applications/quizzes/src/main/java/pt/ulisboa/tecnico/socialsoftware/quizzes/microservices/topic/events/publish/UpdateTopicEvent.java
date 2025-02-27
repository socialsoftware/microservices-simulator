package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.events.publish;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

import jakarta.persistence.Entity;

@Entity
public class UpdateTopicEvent extends Event {

    private String topicName;

    public UpdateTopicEvent() {
        super();
    }

    public UpdateTopicEvent(Integer topicAggregateId, String topicName) {
        super(topicAggregateId);
        setTopicName(topicName);
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String name) {
        this.topicName = name;
    }
}
