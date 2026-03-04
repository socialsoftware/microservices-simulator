package pt.ulisboa.tecnico.socialsoftware.advanced.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderCustomerDeletedEvent extends Event {
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