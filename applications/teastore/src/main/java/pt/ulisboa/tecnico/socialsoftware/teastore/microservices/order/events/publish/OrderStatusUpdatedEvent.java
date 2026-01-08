package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderStatusUpdatedEvent extends Event {
    private String status;

    public OrderStatusUpdatedEvent() {
    }

    public OrderStatusUpdatedEvent(Integer aggregateId, String status) {
        super(aggregateId);
        setStatus(status);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}