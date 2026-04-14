package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.PaymentStatus;

public class CreatePaymentRequestDto {
    @NotNull
    private OrderDto order;
    @NotNull
    private Double amountInCents;
    @NotNull
    private PaymentStatus status;
    @NotNull
    private String authorizationCode;
    @NotNull
    private String paymentMethod;

    public CreatePaymentRequestDto() {}

    public CreatePaymentRequestDto(OrderDto order, Double amountInCents, PaymentStatus status, String authorizationCode, String paymentMethod) {
        this.order = order;
        this.amountInCents = amountInCents;
        this.status = status;
        this.authorizationCode = authorizationCode;
        this.paymentMethod = paymentMethod;
    }

    public OrderDto getOrder() {
        return order;
    }

    public void setOrder(OrderDto order) {
        this.order = order;
    }
    public Double getAmountInCents() {
        return amountInCents;
    }

    public void setAmountInCents(Double amountInCents) {
        this.amountInCents = amountInCents;
    }
    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }
    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
