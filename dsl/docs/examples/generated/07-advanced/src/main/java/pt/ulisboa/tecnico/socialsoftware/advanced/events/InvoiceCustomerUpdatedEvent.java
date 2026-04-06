package pt.ulisboa.tecnico.socialsoftware.advanced.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class InvoiceCustomerUpdatedEvent extends Event {
    @Column(name = "invoice_customer_updated_event_customer_aggregate_id")
    private Integer customerAggregateId;
    @Column(name = "invoice_customer_updated_event_customer_version")
    private Integer customerVersion;
    @Column(name = "invoice_customer_updated_event_customer_name")
    private String customerName;
    @Column(name = "invoice_customer_updated_event_customer_email")
    private String customerEmail;

    public InvoiceCustomerUpdatedEvent() {
        super();
    }

    public InvoiceCustomerUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public InvoiceCustomerUpdatedEvent(Integer aggregateId, Integer customerAggregateId, Integer customerVersion, String customerName, String customerEmail) {
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