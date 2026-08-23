package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate;

public interface OrderFactory {
    Order createOrder(Integer aggregateId, OrderDto orderDto);

    Order createOrderCopy(Order existing);

    OrderDto createOrderDto(Order order);
}
