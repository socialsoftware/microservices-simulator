package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.Cart;

public class CartDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private CartUserDto user;
    private Double totalInCents;
    private Integer itemCount;
    private Boolean checkedOut;

    public CartDto() {
    }

    public CartDto(Cart cart) {
        this.aggregateId = cart.getAggregateId();
        this.version = cart.getVersion();
        this.state = cart.getState();
        this.user = cart.getUser() != null ? new CartUserDto(cart.getUser()) : null;
        this.totalInCents = cart.getTotalInCents();
        this.itemCount = cart.getItemCount();
        this.checkedOut = cart.getCheckedOut();
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

    public CartUserDto getUser() {
        return user;
    }

    public void setUser(CartUserDto user) {
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

    public Boolean getCheckedOut() {
        return checkedOut;
    }

    public void setCheckedOut(Boolean checkedOut) {
        this.checkedOut = checkedOut;
    }
}