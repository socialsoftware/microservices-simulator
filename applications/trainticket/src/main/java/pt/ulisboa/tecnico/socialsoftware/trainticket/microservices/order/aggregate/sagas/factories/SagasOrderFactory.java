package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.sagas.SagaOrder;

@Service
@Profile("sagas")
public class SagasOrderFactory implements OrderFactory {
    @Override
    public SagaOrder createOrder(Integer aggregateId, OrderDto orderDto) {
        return new SagaOrder(aggregateId, orderDto);
    }

    @Override
    public SagaOrder createOrderCopy(Order existing) {
        return new SagaOrder((SagaOrder) existing);
    }

    @Override
    public OrderDto createOrderDto(Order order) {
        return new OrderDto(order);
    }
}
