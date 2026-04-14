package pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.Invoice;

public class InvoiceDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private InvoiceOrderDto order;
    private InvoiceCustomerDto customer;
    private Double totalAmount;
    private LocalDateTime issuedAt;
    private Boolean paid;

    public InvoiceDto() {
    }

    public InvoiceDto(Invoice invoice) {
        this.aggregateId = invoice.getAggregateId();
        this.version = invoice.getVersion();
        this.state = invoice.getState();
        this.order = invoice.getOrder() != null ? new InvoiceOrderDto(invoice.getOrder()) : null;
        this.customer = invoice.getCustomer() != null ? new InvoiceCustomerDto(invoice.getCustomer()) : null;
        this.totalAmount = invoice.getTotalAmount();
        this.issuedAt = invoice.getIssuedAt();
        this.paid = invoice.getPaid();
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

    public InvoiceOrderDto getOrder() {
        return order;
    }

    public void setOrder(InvoiceOrderDto order) {
        this.order = order;
    }

    public InvoiceCustomerDto getCustomer() {
        return customer;
    }

    public void setCustomer(InvoiceCustomerDto customer) {
        this.customer = customer;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }
}