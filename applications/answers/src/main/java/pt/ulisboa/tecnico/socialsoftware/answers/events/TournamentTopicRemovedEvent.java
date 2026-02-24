package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TournamentTopicRemovedEvent extends Event {
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