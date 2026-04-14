package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class CartUpdatedEvent extends Event {
    @Column(name = "cart_updated_event_total_in_cents")
    private Double totalInCents;
    @Column(name = "cart_updated_event_item_count")
    private Integer itemCount;
    @Column(name = "cart_updated_event_checked_out")
    private Boolean checkedOut;

    public CartUpdatedEvent() {
        super();
    }

    public CartUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public CartUpdatedEvent(Integer aggregateId, Double totalInCents, Integer itemCount, Boolean checkedOut) {
        super(aggregateId);
        setTotalInCents(totalInCents);
        setItemCount(itemCount);
        setCheckedOut(checkedOut);
    }

    public Double getTotalInCents() {
        return totalInCents;
    }

    public void setTotalInCents(Double totalInCents) {
        this.totalInCents = totalInCents;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public Boolean getCheckedOut() {
        return checkedOut;
    }

    public void setCheckedOut(Boolean checkedOut) {
        this.checkedOut = checkedOut;
    }

}