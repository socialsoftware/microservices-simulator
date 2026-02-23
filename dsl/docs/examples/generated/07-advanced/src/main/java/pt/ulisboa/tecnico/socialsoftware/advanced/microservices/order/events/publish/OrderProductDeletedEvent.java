package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderProductDeletedEvent extends Event {
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