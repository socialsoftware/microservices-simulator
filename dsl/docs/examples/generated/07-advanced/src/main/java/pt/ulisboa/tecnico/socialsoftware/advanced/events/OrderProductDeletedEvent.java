package pt.ulisboa.tecnico.socialsoftware.advanced.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class OrderProductDeletedEvent extends Event {
    @Column(name = "order_product_deleted_event_product_aggregate_id")
    private Integer productAggregateId;

    public OrderProductDeletedEvent() {
        super();
    }

    public OrderProductDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderProductDeletedEvent(Integer aggregateId, Integer productAggregateId) {
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