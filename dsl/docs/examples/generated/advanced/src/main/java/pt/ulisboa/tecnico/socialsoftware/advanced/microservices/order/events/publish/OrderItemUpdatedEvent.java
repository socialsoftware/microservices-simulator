package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderItemUpdatedEvent extends Event {
    private Integer key;

    public OrderItemUpdatedEvent() {
        super();
    }

    public OrderItemUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderItemUpdatedEvent(Integer aggregateId, Integer key) {
        super(aggregateId);
        setKey(key);
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

}