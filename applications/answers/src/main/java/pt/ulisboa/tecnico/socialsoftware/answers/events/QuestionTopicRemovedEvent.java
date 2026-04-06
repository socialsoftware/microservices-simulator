package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class QuestionTopicRemovedEvent extends Event {
    @Column(name = "question_topic_removed_event_topic_aggregate_id")
    private Integer topicAggregateId;

    public QuestionTopicRemovedEvent() {
        super();
    }

    public QuestionTopicRemovedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public QuestionTopicRemovedEvent(Integer aggregateId, Integer topicAggregateId) {
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