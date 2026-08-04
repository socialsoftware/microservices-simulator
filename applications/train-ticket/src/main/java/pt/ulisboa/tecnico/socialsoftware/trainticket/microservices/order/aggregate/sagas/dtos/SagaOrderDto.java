package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.sagas.dtos;

import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.sagas.SagaOrder;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

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