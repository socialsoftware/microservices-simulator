package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TournamentTopicUpdatedEvent extends Event {
    private Integer topicAggregateId;
    private Integer topicVersion;
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