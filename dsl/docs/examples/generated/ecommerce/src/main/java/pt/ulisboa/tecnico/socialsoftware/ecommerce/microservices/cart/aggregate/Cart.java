package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.events.subscribe.CartSubscribesUserDeletedCartUserExists;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.events.subscribe.CartSubscribesUserUpdated;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartUserDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Cart extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "cart")
    private CartUser user;
    private Double totalInCents;
    private Integer itemCount;
    private Boolean checkedOut;

    public Cart() {

    }

    public Cart(Integer aggregateId, CartDto cartDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTotalInCents(cartDto.getTotalInCents());
        setItemCount(cartDto.getItemCount());
        setCheckedOut(cartDto.getCheckedOut());
        setUser(cartDto.getUser() != null ? new CartUser(cartDto.getUser()) : null);
    }


    public Cart(Cart other) {
        super(other);
        setUser(new CartUser(other.getUser()));
        setTotalInCents(other.getTotalInCents());
        setItemCount(other.getItemCount());
        setCheckedOut(other.getCheckedOut());
    }

    public CartUser getUser() {
        return user;
    }

    public void setUser(CartUser user) {
        this.user = user;
        if (this.user != null) {
            this.user.setCart(this);
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

    public Boolean getCheckedOut() {
        return checkedOut;
    }

    public void setCheckedOut(Boolean checkedOut) {
        this.checkedOut = checkedOut;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantCartUserExists(eventSubscriptions);
            eventSubscriptions.add(new CartSubscribesUserUpdated());
        }
        return eventSubscriptions;
    }
    private void interInvariantCartUserExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new CartSubscribesUserDeletedCartUserExists(this.getUser()));
    }


    private boolean invariantTotalNonNegative() {
        return totalInCents >= 0.0;
    }

    private boolean invariantItemCountNonNegative() {
        return itemCount >= 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantTotalNonNegative()) {
            throw new SimulatorException(INVARIANT_BREAK, "Cart total cannot be negative");
        }
        if (!invariantItemCountNonNegative()) {
            throw new SimulatorException(INVARIANT_BREAK, "Cart item count cannot be negative");
        }
    }

    public CartDto buildDto() {
        CartDto dto = new CartDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setUser(getUser() != null ? new CartUserDto(getUser()) : null);
        dto.setTotalInCents(getTotalInCents());
        dto.setItemCount(getItemCount());
        dto.setCheckedOut(getCheckedOut());
        return dto;
    }
}