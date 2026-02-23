package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class AnswerUserDeletedEvent extends Event {
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