package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.events.subscribe.OrderSubscribesUserDeletedOrderUserExists;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.events.subscribe.OrderSubscribesUserUpdated;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderUserDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.OrderStatus;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Order extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "order")
    private OrderUser user;
    private Double totalInCents;
    private Integer itemCount;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private String placedAt;

    public Order() {

    }

    public Order(Integer aggregateId, OrderDto orderDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTotalInCents(orderDto.getTotalInCents());
        setItemCount(orderDto.getItemCount());
        setStatus(OrderStatus.valueOf(orderDto.getStatus()));
        setPlacedAt(orderDto.getPlacedAt());
        setUser(orderDto.getUser() != null ? new OrderUser(orderDto.getUser()) : null);
    }


    public Order(Order other) {
        super(other);
        setUser(new OrderUser(other.getUser()));
        setTotalInCents(other.getTotalInCents());
        setItemCount(other.getItemCount());
        setStatus(other.getStatus());
        setPlacedAt(other.getPlacedAt());
    }

    public OrderUser getUser() {
        return user;
    }

    public void setUser(OrderUser user) {
        this.user = user;
        if (this.user != null) {
            this.user.setOrder(this);
        }
    }

    public Double getTotalInCents() {
        return totalInCents;
    }

    public void setTotalInCents(Double totalInCents) {
        this.totalInCents = totalInCents;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(String placedAt) {
        this.placedAt = placedAt;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantOrderUserExists(eventSubscriptions);
            eventSubscriptions.add(new OrderSubscribesUserUpdated());
        }
        return eventSubscriptions;
    }
    private void interInvariantOrderUserExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new OrderSubscribesUserDeletedOrderUserExists(this.getUser()));
    }


    private boolean invariantTotalPositive() {
        return totalInCents > 0.0;
    }

    private boolean invariantHasItems() {
        return itemCount > 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantTotalPositive()) {
            throw new SimulatorException(INVARIANT_BREAK, "Order total must be positive");
        }
        if (!invariantHasItems()) {
            throw new SimulatorException(INVARIANT_BREAK, "Order must contain at least one item");
        }
    }

    public OrderDto buildDto() {
        OrderDto dto = new OrderDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setUser(getUser() != null ? new OrderUserDto(getUser()) : null);
        dto.setTotalInCents(getTotalInCents());
        dto.setItemCount(getItemCount());
        dto.setStatus(getStatus() != null ? getStatus().name() : null);
        dto.setPlacedAt(getPlacedAt());
        return dto;
    }
}