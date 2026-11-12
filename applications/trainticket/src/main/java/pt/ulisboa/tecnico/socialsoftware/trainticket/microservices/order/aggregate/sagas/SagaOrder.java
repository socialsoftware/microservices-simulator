package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.sagas;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderDto;

@Entity
public class SagaOrder extends Order implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaOrder() {
    }

    public SagaOrder(Integer aggregateId, OrderDto orderDto) {
        super(aggregateId, orderDto);
        setSagaState(GenericSagaState.NOT_IN_SAGA);
    }

    public SagaOrder(SagaOrder other) {
        super(other);
        setSagaState(other.getSagaState());
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }

    @Override
    public void setSagaState(SagaState sagaState) {
        this.sagaState = sagaState;
    }
}
