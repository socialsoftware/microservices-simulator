package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class QuestionTopicUpdatedEvent extends Event {
    @Column(name = "question_topic_updated_event_topic_aggregate_id")
    private Integer topicAggregateId;
    @Column(name = "question_topic_updated_event_topic_version")
    private Integer topicVersion;
    @Column(name = "question_topic_updated_event_topic_name")
    private String topicName;
    @Column(name = "question_topic_updated_event_topic_id")
    private Integer topicId;

    public QuestionTopicUpdatedEvent() {
        super();
    }

    public QuestionTopicUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public QuestionTopicUpdatedEvent(Integer aggregateId, Integer topicAggregateId, Integer topicVersion, String topicName, Integer topicId) {
        super(aggregateId);
        setTopicAggregateId(topicAggregateId);
        setTopicVersion(topicVersion);
        setTopicName(topicName);
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

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }

}