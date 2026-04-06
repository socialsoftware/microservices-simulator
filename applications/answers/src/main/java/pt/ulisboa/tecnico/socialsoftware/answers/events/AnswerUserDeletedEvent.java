package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class AnswerUserDeletedEvent extends Event {
    @Column(name = "answer_user_deleted_event_user_aggregate_id")
    private Integer userAggregateId;

    public AnswerUserDeletedEvent() {
        super();
    }

    public AnswerUserDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public AnswerUserDeletedEvent(Integer aggregateId, Integer userAggregateId) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

}