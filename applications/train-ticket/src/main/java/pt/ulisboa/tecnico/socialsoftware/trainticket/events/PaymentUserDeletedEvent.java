package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class PaymentUserDeletedEvent extends Event {
    private Integer userAggregateId;

    public PaymentUserDeletedEvent() {
        super();
    }

    public PaymentUserDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public PaymentUserDeletedEvent(Integer aggregateId, Integer userAggregateId) {
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
