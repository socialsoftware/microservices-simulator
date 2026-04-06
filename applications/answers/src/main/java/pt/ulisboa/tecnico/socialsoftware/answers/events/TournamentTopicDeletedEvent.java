package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class TournamentTopicDeletedEvent extends Event {
    @Column(name = "tournament_topic_deleted_event_topic_aggregate_id")
    private Integer topicAggregateId;

    public TournamentTopicDeletedEvent() {
        super();
    }

    public TournamentTopicDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TournamentTopicDeletedEvent(Integer aggregateId, Integer topicAggregateId) {
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