package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderDto;

@Entity
public class SagaOrder extends Order implements SagaAggregate {
    private SagaState sagaState;

    public SagaOrder() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaOrder(SagaOrder other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaOrder(Integer aggregateId, OrderDto orderDto) {
        super(aggregateId, orderDto);
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