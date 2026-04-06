package pt.ulisboa.tecnico.socialsoftware.advanced.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class OrderItemUpdatedEvent extends Event {
    @Column(name = "\"key\"")
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