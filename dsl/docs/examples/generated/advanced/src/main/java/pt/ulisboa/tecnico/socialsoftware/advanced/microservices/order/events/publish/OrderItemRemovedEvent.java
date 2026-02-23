package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderItemRemovedEvent extends Event {
    private Integer key;

    public OrderItemRemovedEvent() {
        super();
    }

    public OrderItemRemovedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderItemRemovedEvent(Integer aggregateId, Integer key) {
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