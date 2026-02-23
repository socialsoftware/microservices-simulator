package pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.InvoiceCustomer;

public class InvoiceCustomerDto implements Serializable {
    private String name;
    private String email;
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;

    public InvoiceCustomerDto() {
    }

    public InvoiceCustomerDto(InvoiceCustomer invoiceCustomer) {
        this.name = invoiceCustomer.getCustomerName();
        this.email = invoiceCustomer.getCustomerEmail();
        this.aggregateId = invoiceCustomer.getCustomerAggregateId();
        this.version = invoiceCustomer.getCustomerVersion();
        this.state = invoiceCustomer.getCustomerState();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }
}