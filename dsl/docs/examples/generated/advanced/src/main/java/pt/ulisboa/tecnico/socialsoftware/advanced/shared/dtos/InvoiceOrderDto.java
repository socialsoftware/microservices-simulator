package pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos;

import java.io.Serializable;
import java.util.Set;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.InvoiceOrder;

public class InvoiceOrderDto implements Serializable {
    private AggregateState state;
    private Set<Integer> orderProductIds;
    private Integer aggregateId;
    private Integer version;

    public InvoiceOrderDto() {
    }

    public InvoiceOrderDto(InvoiceOrder invoiceOrder) {
        this.state = invoiceOrder.getOrderState();
        this.orderProductIds = invoiceOrder.getOrderProductIds();
        this.aggregateId = invoiceOrder.getOrderAggregateId();
        this.version = invoiceOrder.getOrderVersion();
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public Set<Integer> getOrderProductIds() {
        return orderProductIds;
    }

    public void setOrderProductIds(Set<Integer> orderProductIds) {
        this.orderProductIds = orderProductIds;
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