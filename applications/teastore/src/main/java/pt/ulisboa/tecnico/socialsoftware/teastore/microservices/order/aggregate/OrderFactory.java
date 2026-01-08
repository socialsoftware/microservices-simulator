package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.OrderDto;

public interface OrderFactory {
    Order createOrder(Integer aggregateId, OrderUser user, OrderDto orderDto);
    Order createOrderFromExisting(Order existingOrder);
    OrderDto createOrderDto(Order order);
}
