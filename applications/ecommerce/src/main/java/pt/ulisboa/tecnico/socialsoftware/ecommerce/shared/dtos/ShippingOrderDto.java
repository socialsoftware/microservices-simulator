package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.ShippingOrder;

public class ShippingOrderDto implements Serializable {
    private Double totalInCents;
    private Integer itemCount;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public ShippingOrderDto() {
    }

    public ShippingOrderDto(ShippingOrder shippingOrder) {
        this.totalInCents = shippingOrder.getOrderTotalInCents();
        this.itemCount = shippingOrder.getOrderItemCount();
        this.aggregateId = shippingOrder.getOrderAggregateId();
        this.version = shippingOrder.getOrderVersion();
        this.state = shippingOrder.getOrderState() != null ? shippingOrder.getOrderState().name() : null;
    }

    public Double getTotalInCents() {
        return totalInCents;
    }

    public void setTotalInCents(Double totalInCents) {
        this.totalInCents = totalInCents;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
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