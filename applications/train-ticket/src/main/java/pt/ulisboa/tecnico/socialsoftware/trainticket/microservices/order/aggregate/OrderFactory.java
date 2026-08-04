package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderDto;

public interface OrderFactory {
    Order createOrder(Integer aggregateId, OrderDto orderDto);
    Order createOrderFromExisting(Order existingOrder);
    OrderDto createOrderDto(Order order);
}
