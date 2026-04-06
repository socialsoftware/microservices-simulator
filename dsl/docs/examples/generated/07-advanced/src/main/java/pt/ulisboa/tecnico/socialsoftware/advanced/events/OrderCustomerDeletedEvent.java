package pt.ulisboa.tecnico.socialsoftware.advanced.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class OrderCustomerDeletedEvent extends Event {
    @Column(name = "order_customer_deleted_event_customer_aggregate_id")
    private Integer customerAggregateId;

    public OrderCustomerDeletedEvent() {
        super();
    }

    public OrderCustomerDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderCustomerDeletedEvent(Integer aggregateId, Integer customerAggregateId) {
        super(aggregateId);
        setCustomerAggregateId(customerAggregateId);
    }

    public Integer getCustomerAggregateId() {
        return customerAggregateId;
    }

    public void setCustomerAggregateId(Integer customerAggregateId) {
        this.customerAggregateId = customerAggregateId;
    }

}