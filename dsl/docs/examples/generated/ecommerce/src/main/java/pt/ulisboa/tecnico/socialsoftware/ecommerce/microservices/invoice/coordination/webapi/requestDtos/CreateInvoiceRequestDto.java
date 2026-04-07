package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.InvoiceStatus;

public class CreateInvoiceRequestDto {
    @NotNull
    private OrderDto order;
    @NotNull
    private UserDto user;
    @NotNull
    private String invoiceNumber;
    @NotNull
    private Double amountInCents;
    @NotNull
    private String issuedAt;
    @NotNull
    private InvoiceStatus status;

    public CreateInvoiceRequestDto() {}

    public CreateInvoiceRequestDto(OrderDto order, UserDto user, String invoiceNumber, Double amountInCents, String issuedAt, InvoiceStatus status) {
        this.order = order;
        this.user = user;
        this.invoiceNumber = invoiceNumber;
        this.amountInCents = amountInCents;
        this.issuedAt = issuedAt;
        this.status = status;
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
    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }
    public Double getAmountInCents() {
        return amountInCents;
    }

    public void setAmountInCents(Double amountInCents) {
        this.amountInCents = amountInCents;
    }
    public String getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(String issuedAt) {
        this.issuedAt = issuedAt;
    }
    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }
}
