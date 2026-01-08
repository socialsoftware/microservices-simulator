package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;

@Entity
public abstract class Cart extends Aggregate {
    private Long userId;
    private boolean checkedOut;
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

    public boolean getCheckedOut() {
        return checkedOut;
    }

    public void setCheckedOut(boolean checkedOut) {
        this.checkedOut = checkedOut;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }


    // ============================================================================
    // INVARIANTS
    // ============================================================================

    public boolean invariantTotalNonNegative() {
        return totalPrice >= 0.0;
    }
    @Override
    public void verifyInvariants() {
        if (!(invariantTotalNonNegative())) {
            throw new SimulatorException(INVARIANT_BREAK, getAggregateId());
        }
    }

}