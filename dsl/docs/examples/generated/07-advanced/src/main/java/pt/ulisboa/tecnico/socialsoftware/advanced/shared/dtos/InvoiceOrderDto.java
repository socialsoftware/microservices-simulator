package pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos;

import java.io.Serializable;
import java.util.Set;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.InvoiceOrder;

public class InvoiceOrderDto implements Serializable {
    private String state;
    private Set<String> orderItemKeys;
    private Integer aggregateId;
    private Integer version;

    public InvoiceOrderDto() {
    }

    public InvoiceOrderDto(InvoiceOrder invoiceOrder) {
        this.state = invoiceOrder.getOrderState() != null ? invoiceOrder.getOrderState().name() : null;
        this.orderItemKeys = invoiceOrder.getOrderItemKeys();
        this.aggregateId = invoiceOrder.getOrderAggregateId();
        this.version = invoiceOrder.getOrderVersion();
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Set<String> getOrderItemKeys() {
        return orderItemKeys;
    }

    public void setOrderItemKeys(Set<String> orderItemKeys) {
        this.orderItemKeys = orderItemKeys;
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
}