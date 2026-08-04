package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.PaymentType;

public class CreatePaymentRequestDto {
    @NotNull
    private OrderDto order;
    @NotNull
    private UserDto user;
    @NotNull
    private Double amount;
    @NotNull
    private PaymentType type;
    @NotNull
    private String paymentDate;

    public CreatePaymentRequestDto() {}

    public CreatePaymentRequestDto(OrderDto order, UserDto user, Double amount, PaymentType type, String paymentDate) {
        this.order = order;
        this.user = user;
        this.amount = amount;
        this.type = type;
        this.paymentDate = paymentDate;
    }

    public OrderDto getOrder() {
        return order;
    }

    public void setOrder(OrderDto order) {
        this.order = order;
    }
    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
    public PaymentType getType() {
        return type;
    }

    public void setType(PaymentType type) {
        this.type = type;
    }
    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }
}
