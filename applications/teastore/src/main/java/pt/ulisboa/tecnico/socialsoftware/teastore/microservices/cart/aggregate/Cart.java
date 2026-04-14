package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Cart extends Aggregate {
    private Long userId;
    private Boolean checkedOut;
    private Double totalPrice;

    public Cart() {

    }

    public Cart(Integer aggregateId, CartDto cartDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setUserId(cartDto.getUserId());
        setCheckedOut(cartDto.getCheckedOut());
        setTotalPrice(cartDto.getTotalPrice());
    }


    public Cart(Cart other) {
        super(other);
        setUserId(other.getUserId());
        setCheckedOut(other.getCheckedOut());
        setTotalPrice(other.getTotalPrice());
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }



    private boolean invariantRule0() {
        return totalPrice >= 0.0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Cart total price cannot be negative");
        }
    }

    public CartDto buildDto() {
        CartDto dto = new CartDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setUserId(getUserId());
        dto.setCheckedOut(getCheckedOut());
        dto.setTotalPrice(getTotalPrice());
        return dto;
    }
}