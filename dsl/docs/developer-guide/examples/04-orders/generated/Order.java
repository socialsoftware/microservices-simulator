package com.generated.abstractions.microservices.order.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import com.generated.ms.domain.aggregate.Aggregate;
import com.generated.ms.domain.aggregate.Aggregate.AggregateState;
import com.generated.ms.domain.event.EventSubscription;

import com.generated.abstractions.microservices.order.events.subscribe.OrderSubscribesCustomerDeleted;
import com.generated.abstractions.microservices.order.events.subscribe.OrderSubscribesCustomerUpdated;

import com.generated.abstractions.shared.dtos.OrderCustomerDto;
import com.generated.abstractions.shared.dtos.OrderDto;

@Entity
public abstract class Order extends Aggregate {
    private String coffeeType;
    private Integer quantity;
    private Double totalPrice;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "order")
    private OrderCustomer customer;

    public Order() {

    }

    public Order(Integer aggregateId, OrderDto orderDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setCoffeeType(orderDto.getCoffeeType());
        setQuantity(orderDto.getQuantity());
        setTotalPrice(orderDto.getTotalPrice());
        setCustomer(orderDto.getCustomer() != null ? new OrderCustomer(orderDto.getCustomer()) : null);
    }


    public Order(Order other) {
        super(other);
        setCoffeeType(other.getCoffeeType());
        setQuantity(other.getQuantity());
        setTotalPrice(other.getTotalPrice());
        setCustomer(new OrderCustomer(other.getCustomer()));
    }

    public String getCoffeeType() {
        return coffeeType;
    }

    public void setCoffeeType(String coffeeType) {
        this.coffeeType = coffeeType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            eventSubscriptions.add(new OrderSubscribesCustomerDeleted());
            eventSubscriptions.add(new OrderSubscribesCustomerUpdated());
        }
        return eventSubscriptions;
    }


    @Override
    public void verifyInvariants() {
        // No invariants defined
    }

    public OrderDto buildDto() {
        OrderDto dto = new OrderDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setCoffeeType(getCoffeeType() != null ? getCoffeeType().toString() : null);
        dto.setQuantity(getQuantity());
        dto.setTotalPrice(getTotalPrice());
        dto.setCustomer(getCustomer() != null ? new OrderCustomerDto(getCustomer()) : null);
        return dto;
    }
}