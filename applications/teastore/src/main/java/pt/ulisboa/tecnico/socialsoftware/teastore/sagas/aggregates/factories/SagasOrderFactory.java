package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.OrderFactory;
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.SagaOrder;
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.dtos.SagaOrderDto;

@Service
@Profile("sagas")
public class SagasOrderFactory extends OrderFactory {
@Override
public Order createOrder(Integer aggregateId, OrderDto orderDto) {
return new SagaOrder(orderDto);
}

@Override
public Order createOrderFromExisting(Order existingOrder) {
return new SagaOrder((SagaOrder) existingOrder);
}

@Override
public OrderDto createOrderDto(Order order) {
return new SagaOrderDto(order);
}
}