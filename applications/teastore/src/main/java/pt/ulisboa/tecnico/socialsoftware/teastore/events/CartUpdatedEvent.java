package pt.ulisboa.tecnico.socialsoftware.teastore.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class CartUpdatedEvent extends Event {
    @Column(name = "cart_updated_event_user_id")
    private Long userId;
    @Column(name = "cart_updated_event_checked_out")
    private Boolean checkedOut;
    @Column(name = "cart_updated_event_total_price")
    private Double totalPrice;

    public CartUpdatedEvent() {
        super();
    }

    public CartUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
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