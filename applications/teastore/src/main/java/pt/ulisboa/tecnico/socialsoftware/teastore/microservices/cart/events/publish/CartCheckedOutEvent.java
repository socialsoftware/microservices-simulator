package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CartCheckedOutEvent extends Event {
    private Long userId;
    private Double totalPrice;

    public CartCheckedOutEvent() {
    }

    public CartCheckedOutEvent(Integer aggregateId, Long userId, Double totalPrice) {
        super(aggregateId);
        setUserId(userId);
        setTotalPrice(totalPrice);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

}