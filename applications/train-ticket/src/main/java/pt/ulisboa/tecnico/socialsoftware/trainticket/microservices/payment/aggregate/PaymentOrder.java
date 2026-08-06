package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PaymentOrderDto;

@Entity
public class PaymentOrder {
    @Id
    @GeneratedValue
    private Long id;
    private Integer orderAggregateId;
    private Long orderVersion;
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
        setOrderAggregateId(paymentOrderDto.getAggregateId());
        setOrderVersion(paymentOrderDto.getVersion());
        setOrderState(paymentOrderDto.getState() != null ? AggregateState.valueOf(paymentOrderDto.getState()) : null);
    }

    public PaymentOrder(PaymentOrder other) {
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

    public Integer getOrderAggregateId() {
        return orderAggregateId;
    }

    public void setOrderAggregateId(Integer orderAggregateId) {
        this.orderAggregateId = orderAggregateId;
    }

    public Long getOrderVersion() {
        return orderVersion;
    }

    public void setOrderVersion(Long orderVersion) {
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
        dto.setAggregateId(getOrderAggregateId());
        dto.setVersion(getOrderVersion());
        dto.setState(getOrderState() != null ? getOrderState().name() : null);
        return dto;
    }
}