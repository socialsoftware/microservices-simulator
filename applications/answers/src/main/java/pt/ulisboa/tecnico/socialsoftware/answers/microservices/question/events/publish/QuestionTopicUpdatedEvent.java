package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class QuestionTopicUpdatedEvent extends Event {
    private Integer topicAggregateId;
    private Integer topicVersion;
    private Integer topicId;

    public QuestionTopicUpdatedEvent() {
        super();
    }

    public QuestionTopicUpdatedEvent(Integer aggregateId, Integer topicAggregateId, Integer topicVersion, Integer topicId) {
        super(aggregateId);
        setTopicAggregateId(topicAggregateId);
        setTopicVersion(topicVersion);
        setTopicId(topicId);
    }

    public Integer getTopicAggregateId() {
        return topicAggregateId;
    }

    public void setTopicAggregateId(Integer topicAggregateId) {
        this.topicAggregateId = topicAggregateId;
    }

    public Integer getTopicVersion() {
        return topicVersion;
    }

    public void setTopicVersion(Integer topicVersion) {
        this.topicVersion = topicVersion;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }

}