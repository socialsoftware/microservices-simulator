package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.Cart;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartDto;

@Entity
public class SagaCart extends Cart implements SagaAggregate {
    private SagaState sagaState;

    public SagaCart() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaCart(SagaCart other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaCart(Integer aggregateId, CartDto cartDto) {
        super(aggregateId, cartDto);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = state;
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }
}