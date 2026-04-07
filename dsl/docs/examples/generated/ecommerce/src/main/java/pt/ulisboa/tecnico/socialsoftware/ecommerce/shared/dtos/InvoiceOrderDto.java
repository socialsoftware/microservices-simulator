package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.InvoiceOrder;

public class InvoiceOrderDto implements Serializable {
    private Double totalInCents;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public InvoiceOrderDto() {
    }

    public InvoiceOrderDto(InvoiceOrder invoiceOrder) {
        this.totalInCents = invoiceOrder.getOrderTotalInCents();
        this.aggregateId = invoiceOrder.getOrderAggregateId();
        this.version = invoiceOrder.getOrderVersion();
        this.state = invoiceOrder.getOrderState() != null ? invoiceOrder.getOrderState().name() : null;
    }

    public Double getTotalInCents() {
        return totalInCents;
    }

    public void setTotalInCents(Double totalInCents) {
        this.totalInCents = totalInCents;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}