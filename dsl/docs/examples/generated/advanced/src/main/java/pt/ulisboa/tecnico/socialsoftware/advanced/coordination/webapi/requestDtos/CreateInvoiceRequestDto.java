package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;
import java.time.LocalDateTime;

public class CreateInvoiceRequestDto {
    @NotNull
    private OrderDto order;
    @NotNull
    private CustomerDto customer;
    @NotNull
    private Double totalAmount;
    @NotNull
    private LocalDateTime issuedAt;
    @NotNull
    private Boolean paid;

    public CreateInvoiceRequestDto() {}

    public CreateInvoiceRequestDto(OrderDto order, CustomerDto customer, Double totalAmount, LocalDateTime issuedAt, Boolean paid) {
        this.order = order;
        this.customer = customer;
        this.totalAmount = totalAmount;
        this.issuedAt = issuedAt;
        this.paid = paid;
    }

    public OrderDto getOrder() {
        return order;
    }

    public void setOrder(OrderDto order) {
        this.order = order;
    }
    public CustomerDto getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerDto customer) {
        this.customer = customer;
    }
    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }
    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }
}
