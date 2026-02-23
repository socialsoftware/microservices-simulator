package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderCustomerUpdatedEvent extends Event {
    private Integer customerAggregateId;
    private Integer customerVersion;
    private String customerName;
    private String customerEmail;

    public OrderCustomerUpdatedEvent() {
        super();
    }

    public OrderCustomerUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderCustomerUpdatedEvent(Integer aggregateId, Integer customerAggregateId, Integer customerVersion, String customerName, String customerEmail) {
        super(aggregateId);
        setCustomerAggregateId(customerAggregateId);
        setCustomerVersion(customerVersion);
        setCustomerName(customerName);
        setCustomerEmail(customerEmail);
    }

    public Integer getCustomerAggregateId() {
        return customerAggregateId;
    }

    public void setCustomerAggregateId(Integer customerAggregateId) {
        this.customerAggregateId = customerAggregateId;
    }

    public Integer getCustomerVersion() {
        return customerVersion;
    }

    public void setCustomerVersion(Integer customerVersion) {
        this.customerVersion = customerVersion;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

}