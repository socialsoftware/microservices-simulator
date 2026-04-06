package pt.ulisboa.tecnico.socialsoftware.advanced.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class OrderProductRemovedEvent extends Event {
    @Column(name = "order_product_removed_event_product_aggregate_id")
    private Integer productAggregateId;

    public OrderProductRemovedEvent() {
        super();
    }

    public OrderProductRemovedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderProductRemovedEvent(Integer aggregateId, Integer productAggregateId) {
        super(aggregateId);
        setProductAggregateId(productAggregateId);
    }

    public Integer getProductAggregateId() {
        return productAggregateId;
    }

    public void setProductAggregateId(Integer productAggregateId) {
        this.productAggregateId = productAggregateId;
    }

}