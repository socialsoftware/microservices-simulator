package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class CartUserDeletedEvent extends Event {
    @Column(name = "cart_user_deleted_event_user_aggregate_id")
    private Integer userAggregateId;

    public CartUserDeletedEvent() {
        super();
    }

    public CartUserDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public CartUserDeletedEvent(Integer aggregateId, Integer userAggregateId) {
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