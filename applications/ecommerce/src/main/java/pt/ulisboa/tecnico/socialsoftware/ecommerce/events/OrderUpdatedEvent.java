package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class OrderUpdatedEvent extends Event {
    @Column(name = "order_updated_event_total_in_cents")
    private Double totalInCents;
    @Column(name = "order_updated_event_item_count")
    private Integer itemCount;
    @Column(name = "order_updated_event_placed_at")
    private String placedAt;

    public OrderUpdatedEvent() {
        super();
    }

    public OrderUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderUpdatedEvent(Integer aggregateId, Double totalInCents, Integer itemCount, String placedAt) {
        super(aggregateId);
        setTotalInCents(totalInCents);
        setItemCount(itemCount);
        setPlacedAt(placedAt);
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

    public String getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(String placedAt) {
        this.placedAt = placedAt;
    }

}