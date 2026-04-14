package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.Shipping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ShippingDto;

@Entity
public class SagaShipping extends Shipping implements SagaAggregate {
    private SagaState sagaState;

    public SagaShipping() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaShipping(SagaShipping other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaShipping(Integer aggregateId, ShippingDto shippingDto) {
        super(aggregateId, shippingDto);
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