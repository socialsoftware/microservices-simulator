package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class TournamentTopicRemovedEvent extends Event {
    @Column(name = "tournament_topic_removed_event_topic_aggregate_id")
    private Integer topicAggregateId;

    public TournamentTopicRemovedEvent() {
        super();
    }

    public TournamentTopicRemovedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TournamentTopicRemovedEvent(Integer aggregateId, Integer topicAggregateId) {
        super(aggregateId);
        setTopicAggregateId(topicAggregateId);
    }

    public Integer getTopicAggregateId() {
        return topicAggregateId;
    }

    public void setTopicAggregateId(Integer topicAggregateId) {
        this.topicAggregateId = topicAggregateId;
    }

}