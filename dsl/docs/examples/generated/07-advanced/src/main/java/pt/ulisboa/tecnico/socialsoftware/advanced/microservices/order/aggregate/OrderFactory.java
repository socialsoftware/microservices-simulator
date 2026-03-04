package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate;

import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderDto;

public interface OrderFactory {
    Order createOrder(Integer aggregateId, OrderDto orderDto);
    Order createOrderFromExisting(Order existingOrder);
    OrderDto createOrderDto(Order order);
}
