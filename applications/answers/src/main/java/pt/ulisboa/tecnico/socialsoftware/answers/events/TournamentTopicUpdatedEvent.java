package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class TournamentTopicUpdatedEvent extends Event {
    @Column(name = "tournament_topic_updated_event_topic_aggregate_id")
    private Integer topicAggregateId;
    @Column(name = "tournament_topic_updated_event_topic_version")
    private Integer topicVersion;
    @Column(name = "tournament_topic_updated_event_topic_name")
    private String topicName;

    public TournamentTopicUpdatedEvent() {
        super();
    }

    public TournamentTopicUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TournamentTopicUpdatedEvent(Integer aggregateId, Integer topicAggregateId, Integer topicVersion, String topicName) {
        super(aggregateId);
        setTopicAggregateId(topicAggregateId);
        setTopicVersion(topicVersion);
        setTopicName(topicName);
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

}