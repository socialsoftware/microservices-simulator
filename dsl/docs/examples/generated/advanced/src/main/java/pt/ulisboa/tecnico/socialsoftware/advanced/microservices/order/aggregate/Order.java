package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
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

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Order extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "order")
    private OrderCustomer customer;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "order")
    private Set<OrderProduct> products = new HashSet<>();
    private Double totalAmount;
    private LocalDateTime orderDate;

    public Order() {

    }

    public Order(Integer aggregateId, OrderDto orderDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTotalAmount(orderDto.getTotalAmount());
        setOrderDate(orderDto.getOrderDate());
        setCustomer(orderDto.getCustomer() != null ? new OrderCustomer(orderDto.getCustomer()) : null);
        setProducts(orderDto.getProducts() != null ? orderDto.getProducts().stream().map(OrderProduct::new).collect(Collectors.toSet()) : null);
    }


    public Order(Order other) {
        super(other);
        setCustomer(new OrderCustomer(other.getCustomer()));
        setProducts(other.getProducts().stream().map(OrderProduct::new).collect(Collectors.toSet()));
        setTotalAmount(other.getTotalAmount());
        setOrderDate(other.getOrderDate());
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


    private boolean invariantCustomerNotNull() {
        return this.customer != null;
    }

    private boolean invariantTotalPositive() {
        return totalAmount > 0.0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantCustomerNotNull()) {
            throw new SimulatorException(INVARIANT_BREAK, "Order must have a customer");
        }
        if (!invariantTotalPositive()) {
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
        dto.setTotalAmount(getTotalAmount());
        dto.setOrderDate(getOrderDate());
        return dto;
    }
}