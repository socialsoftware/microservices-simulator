package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ShippingOrderDto;

@Entity
public class ShippingOrder {
    @Id
    @GeneratedValue
    private Long id;
    private Double orderTotalInCents;
    private Integer orderItemCount;
    private Integer orderAggregateId;
    private Integer orderVersion;
    private AggregateState orderState;
    @OneToOne
    private Shipping shipping;

    public ShippingOrder() {

    }

    public ShippingOrder(OrderDto orderDto) {
        setOrderAggregateId(orderDto.getAggregateId());
        setOrderVersion(orderDto.getVersion());
        setOrderState(orderDto.getState());
    }

    public ShippingOrder(ShippingOrderDto shippingOrderDto) {
        setOrderTotalInCents(shippingOrderDto.getTotalInCents());
        setOrderItemCount(shippingOrderDto.getItemCount());
        setOrderAggregateId(shippingOrderDto.getAggregateId());
        setOrderVersion(shippingOrderDto.getVersion());
        setOrderState(shippingOrderDto.getState() != null ? AggregateState.valueOf(shippingOrderDto.getState()) : null);
    }

    public ShippingOrder(ShippingOrder other) {
        setOrderTotalInCents(other.getOrderTotalInCents());
        setOrderItemCount(other.getOrderItemCount());
        setOrderAggregateId(other.getOrderAggregateId());
        setOrderVersion(other.getOrderVersion());
        setOrderState(other.getOrderState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getOrderTotalInCents() {
        return orderTotalInCents;
    }

    public void setOrderTotalInCents(Double orderTotalInCents) {
        this.orderTotalInCents = orderTotalInCents;
    }

    public Integer getOrderItemCount() {
        return orderItemCount;
    }

    public void setOrderItemCount(Integer orderItemCount) {
        this.orderItemCount = orderItemCount;
    }

    public Integer getOrderAggregateId() {
        return orderAggregateId;
    }

    public void setOrderAggregateId(Integer orderAggregateId) {
        this.orderAggregateId = orderAggregateId;
    }

    public Integer getOrderVersion() {
        return orderVersion;
    }

    public void setOrderVersion(Integer orderVersion) {
        this.orderVersion = orderVersion;
    }

    public AggregateState getOrderState() {
        return orderState;
    }

    public void setOrderState(AggregateState orderState) {
        this.orderState = orderState;
    }

    public Shipping getShipping() {
        return shipping;
    }

    public void setShipping(Shipping shipping) {
        this.shipping = shipping;
    }




    public ShippingOrderDto buildDto() {
        ShippingOrderDto dto = new ShippingOrderDto();
        dto.setTotalInCents(getOrderTotalInCents());
        dto.setItemCount(getOrderItemCount());
        dto.setAggregateId(getOrderAggregateId());
        dto.setVersion(getOrderVersion());
        dto.setState(getOrderState() != null ? getOrderState().name() : null);
        return dto;
    }
}