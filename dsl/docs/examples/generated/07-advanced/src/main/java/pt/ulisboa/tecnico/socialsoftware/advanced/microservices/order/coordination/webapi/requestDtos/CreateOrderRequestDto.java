package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.ProductDto;
import java.util.Set;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderItemDto;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.enums.OrderStatus;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.enums.PaymentMethod;

public class CreateOrderRequestDto {
    @NotNull
    private CustomerDto customer;
    @NotNull
    private Set<ProductDto> products;
    private Set<OrderItemDto> items;
    @NotNull
    private Double totalAmount;
    @NotNull
    private LocalDateTime orderDate;
    @NotNull
    private OrderStatus status;
    @NotNull
    private PaymentMethod paymentMethod;

    public CreateOrderRequestDto() {}

    public CreateOrderRequestDto(CustomerDto customer, Set<ProductDto> products, Set<OrderItemDto> items, Double totalAmount, LocalDateTime orderDate, OrderStatus status, PaymentMethod paymentMethod) {
        this.customer = customer;
        this.products = products;
        this.items = items;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.status = status;
        this.paymentMethod = paymentMethod;
    }

    public CustomerDto getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerDto customer) {
        this.customer = customer;
    }
    public Set<ProductDto> getProducts() {
        return products;
    }

    public void setProducts(Set<ProductDto> products) {
        this.products = products;
    }
    public Set<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(Set<OrderItemDto> items) {
        this.items = items;
    }
    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }
    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
