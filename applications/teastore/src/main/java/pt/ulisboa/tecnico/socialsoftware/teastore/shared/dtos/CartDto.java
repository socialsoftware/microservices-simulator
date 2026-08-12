package pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.Cart;

public class CartDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private Long userId;
    private Boolean checkedOut;
    private Double totalPrice;

    public CartDto() {
    }

    public CartDto(Cart cart) {
        this.aggregateId = cart.getAggregateId();
        this.version = cart.getVersion();
        this.state = cart.getState();
        this.userId = cart.getUserId();
        this.checkedOut = cart.getCheckedOut();
        this.totalPrice = cart.getTotalPrice();
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getCheckedOut() {
        return checkedOut;
    }

    public void setCheckedOut(Boolean checkedOut) {
        this.checkedOut = checkedOut;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }
}