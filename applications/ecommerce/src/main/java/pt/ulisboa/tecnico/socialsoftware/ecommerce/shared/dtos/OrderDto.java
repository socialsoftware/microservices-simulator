package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.Order;

public class OrderDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private OrderUserDto user;
    private Double totalInCents;
    private Integer itemCount;
    private String status;
    private String placedAt;

    public OrderDto() {
    }

    public OrderDto(Order order) {
        this.aggregateId = order.getAggregateId();
        this.version = order.getVersion();
        this.state = order.getState();
        this.user = order.getUser() != null ? new OrderUserDto(order.getUser()) : null;
        this.totalInCents = order.getTotalInCents();
        this.itemCount = order.getItemCount();
        this.status = order.getStatus() != null ? order.getStatus().name() : null;
        this.placedAt = order.getPlacedAt();
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

    public OrderUserDto getUser() {
        return user;
    }

    public void setUser(OrderUserDto user) {
        this.user = user;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(String placedAt) {
        this.placedAt = placedAt;
    }
}