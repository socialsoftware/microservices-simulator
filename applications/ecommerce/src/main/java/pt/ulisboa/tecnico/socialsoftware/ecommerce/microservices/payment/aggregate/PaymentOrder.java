package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.PaymentOrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.OrderStatus;

@Entity
public class PaymentOrder {
    @Id
    @GeneratedValue
    private Long id;
    private Double orderTotalInCents;
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    private Integer orderAggregateId;
    private Integer orderVersion;
    private AggregateState orderState;
    @OneToOne
    private Payment payment;

    public PaymentOrder() {

    }

    public PaymentOrder(OrderDto orderDto) {
        setOrderAggregateId(orderDto.getAggregateId());
        setOrderVersion(orderDto.getVersion());
        setOrderState(orderDto.getState());
    }

    public PaymentOrder(PaymentOrderDto paymentOrderDto) {
        setOrderTotalInCents(paymentOrderDto.getTotalInCents());
        setOrderStatus(paymentOrderDto.getStatus() != null ? OrderStatus.valueOf(paymentOrderDto.getStatus()) : null);
        setOrderAggregateId(paymentOrderDto.getAggregateId());
        setOrderVersion(paymentOrderDto.getVersion());
        setOrderState(paymentOrderDto.getState() != null ? AggregateState.valueOf(paymentOrderDto.getState()) : null);
    }

    public PaymentOrder(PaymentOrder other) {
        setOrderTotalInCents(other.getOrderTotalInCents());
        setOrderStatus(other.getOrderStatus());
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

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
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

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }




    public PaymentOrderDto buildDto() {
        PaymentOrderDto dto = new PaymentOrderDto();
        dto.setTotalInCents(getOrderTotalInCents());
        dto.setStatus(getOrderStatus() != null ? getOrderStatus().name() : null);
        dto.setAggregateId(getOrderAggregateId());
        dto.setVersion(getOrderVersion());
        dto.setState(getOrderState() != null ? getOrderState().name() : null);
        return dto;
    }
}