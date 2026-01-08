package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CartUpdatedEvent extends Event {
    private Long userId;
    private Boolean checkedOut;
    private Double totalPrice;

    public CartUpdatedEvent() {
    }

    public CartUpdatedEvent(Integer aggregateId, Long userId, Boolean checkedOut, Double totalPrice) {
        super(aggregateId);
        setUserId(userId);
        setCheckedOut(checkedOut);
        setTotalPrice(totalPrice);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getCheckedOut() {
        return checkedOut;
    }

    public void setCheckedOut(Boolean checkedOut) {
        this.checkedOut = checkedOut;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

}