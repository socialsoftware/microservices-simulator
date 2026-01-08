package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.SagaOrder;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaOrderDto extends OrderDto {
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