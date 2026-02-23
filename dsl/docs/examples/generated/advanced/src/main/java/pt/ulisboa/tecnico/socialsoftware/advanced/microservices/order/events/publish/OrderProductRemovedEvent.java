package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderProductRemovedEvent extends Event {
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