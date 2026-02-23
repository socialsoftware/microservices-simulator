package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.ProductDto;
import java.util.Set;
import java.time.LocalDateTime;

public class CreateOrderRequestDto {
    @NotNull
    private CustomerDto customer;
    @NotNull
    private Set<ProductDto> products;
    @NotNull
    private Double totalAmount;
    @NotNull
    private LocalDateTime orderDate;

    public CreateOrderRequestDto() {}

    public CreateOrderRequestDto(CustomerDto customer, Set<ProductDto> products, Double totalAmount, LocalDateTime orderDate) {
        this.customer = customer;
        this.products = products;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
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
}
