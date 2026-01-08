package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CartCreatedEvent extends Event {
    private Long userId;

    public CartCreatedEvent() {
    }

    public CartCreatedEvent(Integer aggregateId, Long userId) {
        super(aggregateId);
        setUserId(userId);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

}