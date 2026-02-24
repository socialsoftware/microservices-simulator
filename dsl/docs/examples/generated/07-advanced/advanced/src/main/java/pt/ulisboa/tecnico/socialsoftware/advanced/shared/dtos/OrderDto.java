package pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.OrderProduct;

public class OrderDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private OrderCustomerDto customer;
    private Set<OrderProductDto> products;
    private Set<OrderItemDto> items;
    private Double totalAmount;
    private LocalDateTime orderDate;
    private String status;
    private String paymentMethod;

    public OrderDto() {
    }

    public OrderDto(Order order) {
        this.aggregateId = order.getAggregateId();
        this.version = order.getVersion();
        this.state = order.getState();
        this.customer = order.getCustomer() != null ? new OrderCustomerDto(order.getCustomer()) : null;
        this.products = order.getProducts() != null ? order.getProducts().stream().map(OrderProduct::buildDto).collect(Collectors.toSet()) : null;
        this.items = order.getItems() != null ? order.getItems().stream().map(OrderItemDto::new).collect(Collectors.toSet()) : null;
        this.totalAmount = order.getTotalAmount();
        this.orderDate = order.getOrderDate();
        this.status = order.getStatus() != null ? order.getStatus().name() : null;
        this.paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null;
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

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public OrderCustomerDto getCustomer() {
        return customer;
    }

    public void setCustomer(OrderCustomerDto customer) {
        this.customer = customer;
    }

    public Set<OrderProductDto> getProducts() {
        return products;
    }

    public void setProducts(Set<OrderProductDto> products) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}