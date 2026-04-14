package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.subscribe.OrderSubscribesCustomerDeletedCustomerExists;

import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderCustomerDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderItemDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.enums.OrderStatus;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.enums.PaymentMethod;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Order extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "order")
    private OrderCustomer customer;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "order")
    private Set<OrderProduct> products = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "order")
    private Set<OrderItem> items = new HashSet<>();
    private Double totalAmount;
    private LocalDateTime orderDate;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    public Order() {

    }

    public Order(Integer aggregateId, OrderDto orderDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTotalAmount(orderDto.getTotalAmount());
        setOrderDate(orderDto.getOrderDate());
        setStatus(OrderStatus.valueOf(orderDto.getStatus()));
        setPaymentMethod(PaymentMethod.valueOf(orderDto.getPaymentMethod()));
        setCustomer(orderDto.getCustomer() != null ? new OrderCustomer(orderDto.getCustomer()) : null);
        setProducts(orderDto.getProducts() != null ? orderDto.getProducts().stream().map(OrderProduct::new).collect(Collectors.toSet()) : null);
        setItems(orderDto.getItems() != null ? orderDto.getItems().stream().map(OrderItem::new).collect(Collectors.toSet()) : null);
    }


    public Order(Order other) {
        super(other);
        setCustomer(other.getCustomer() != null ? new OrderCustomer(other.getCustomer()) : null);
        setProducts(other.getProducts() != null ? other.getProducts().stream().map(OrderProduct::new).collect(Collectors.toSet()) : null);
        setItems(other.getItems() != null ? other.getItems().stream().map(OrderItem::new).collect(Collectors.toSet()) : null);
        setTotalAmount(other.getTotalAmount());
        setOrderDate(other.getOrderDate());
        setStatus(other.getStatus());
        setPaymentMethod(other.getPaymentMethod());
    }

    public OrderCustomer getCustomer() {
        return customer;
    }

    public void setCustomer(OrderCustomer customer) {
        this.customer = customer;
        if (this.customer != null) {
            this.customer.setOrder(this);
        }
    }

    public Set<OrderProduct> getProducts() {
        return products;
    }

    public void setProducts(Set<OrderProduct> products) {
        this.products = products;
        if (this.products != null) {
            this.products.forEach(item -> item.setOrder(this));
        }
    }

    public void addOrderProduct(OrderProduct orderProduct) {
        if (this.products == null) {
            this.products = new HashSet<>();
        }
        this.products.add(orderProduct);
        if (orderProduct != null) {
            orderProduct.setOrder(this);
        }
    }

    public void removeOrderProduct(Long id) {
        if (this.products != null) {
            this.products.removeIf(item -> 
                item.getId() != null && item.getId().equals(id));
        }
    }

    public boolean containsOrderProduct(Long id) {
        if (this.products == null) {
            return false;
        }
        return this.products.stream().anyMatch(item -> 
            item.getId() != null && item.getId().equals(id));
    }

    public OrderProduct findOrderProductById(Long id) {
        if (this.products == null) {
            return null;
        }
        return this.products.stream()
            .filter(item -> item.getId() != null && item.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    public Set<OrderItem> getItems() {
        return items;
    }

    public void setItems(Set<OrderItem> items) {
        this.items = items;
        if (this.items != null) {
            this.items.forEach(item -> item.setOrder(this));
        }
    }

    public void addOrderItem(OrderItem orderItem) {
        if (this.items == null) {
            this.items = new HashSet<>();
        }
        this.items.add(orderItem);
        if (orderItem != null) {
            orderItem.setOrder(this);
        }
    }

    public void removeOrderItem(String id) {
        if (this.items != null) {
            this.items.removeIf(item -> 
                item.getKey() != null && item.getKey().equals(id));
        }
    }

    public boolean containsOrderItem(String id) {
        if (this.items == null) {
            return false;
        }
        return this.items.stream().anyMatch(item -> 
            item.getKey() != null && item.getKey().equals(id));
    }

    public OrderItem findOrderItemById(String id) {
        if (this.items == null) {
            return null;
        }
        return this.items.stream()
            .filter(item -> item.getKey() != null && item.getKey().equals(id))
            .findFirst()
            .orElse(null);
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantCustomerExists(eventSubscriptions);
        }
        return eventSubscriptions;
    }
    private void interInvariantCustomerExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new OrderSubscribesCustomerDeletedCustomerExists(this.getCustomer()));
    }


    private boolean invariantRule0() {
        return this.customer != null;
    }

    private boolean invariantRule1() {
        return totalAmount > 0.0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Order must have a customer");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Order total must be positive");
        }
    }

    public OrderDto buildDto() {
        OrderDto dto = new OrderDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setCustomer(getCustomer() != null ? new OrderCustomerDto(getCustomer()) : null);
        dto.setProducts(getProducts() != null ? getProducts().stream().map(OrderProduct::buildDto).collect(Collectors.toSet()) : null);
        dto.setItems(getItems() != null ? getItems().stream().map(OrderItemDto::new).collect(Collectors.toSet()) : null);
        dto.setTotalAmount(getTotalAmount());
        dto.setOrderDate(getOrderDate());
        dto.setStatus(getStatus() != null ? getStatus().name() : null);
        dto.setPaymentMethod(getPaymentMethod() != null ? getPaymentMethod().name() : null);
        return dto;
    }
}