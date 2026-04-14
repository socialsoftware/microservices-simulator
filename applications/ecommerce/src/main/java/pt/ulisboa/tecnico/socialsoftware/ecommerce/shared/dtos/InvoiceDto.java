package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.Invoice;

public class InvoiceDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private InvoiceOrderDto order;
    private InvoiceUserDto user;
    private String invoiceNumber;
    private Double amountInCents;
    private String issuedAt;
    private String status;

    public InvoiceDto() {
    }

    public InvoiceDto(Invoice invoice) {
        this.aggregateId = invoice.getAggregateId();
        this.version = invoice.getVersion();
        this.state = invoice.getState();
        this.order = invoice.getOrder() != null ? new InvoiceOrderDto(invoice.getOrder()) : null;
        this.user = invoice.getUser() != null ? new InvoiceUserDto(invoice.getUser()) : null;
        this.invoiceNumber = invoice.getInvoiceNumber();
        this.amountInCents = invoice.getAmountInCents();
        this.issuedAt = invoice.getIssuedAt();
        this.status = invoice.getStatus() != null ? invoice.getStatus().name() : null;
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

    public InvoiceUserDto getUser() {
        return user;
    }

    public void setUser(InvoiceUserDto user) {
        this.user = user;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Double getAmountInCents() {
        return amountInCents;
    }

    public void setAmountInCents(Double amountInCents) {
        this.amountInCents = amountInCents;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(String issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}