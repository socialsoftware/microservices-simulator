package pt.ulisboa.tecnico.socialsoftware.advanced.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class OrderItemRemovedEvent extends Event {
    @Column(name = "\"key\"")
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