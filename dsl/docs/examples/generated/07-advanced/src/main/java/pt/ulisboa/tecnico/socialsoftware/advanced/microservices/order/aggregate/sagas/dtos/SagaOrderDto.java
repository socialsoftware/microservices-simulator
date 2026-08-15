package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.sagas.SagaOrder;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

public class SagaOrderDto extends OrderDto {
@Convert(converter = SagaStateConverter.class)
private SagaState sagaState;

public SagaOrderDto(Order order) {
super((Order) order);
this.sagaState = ((SagaOrder)order).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}