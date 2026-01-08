package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.OrderUser;

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

    public SagaOrder(Integer aggregateId, OrderDto orderDto, OrderUser user) {
        super(aggregateId, orderDto, user);
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