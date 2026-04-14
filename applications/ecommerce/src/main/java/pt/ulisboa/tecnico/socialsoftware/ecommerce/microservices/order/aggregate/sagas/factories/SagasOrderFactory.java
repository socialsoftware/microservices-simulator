package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.OrderFactory;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.sagas.SagaOrder;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.sagas.dtos.SagaOrderDto;

@Service
@Profile("sagas")
public class SagasOrderFactory implements OrderFactory {
    @Override
    public Order createOrder(Integer aggregateId, OrderDto orderDto) {
        return new SagaOrder(aggregateId, orderDto);
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