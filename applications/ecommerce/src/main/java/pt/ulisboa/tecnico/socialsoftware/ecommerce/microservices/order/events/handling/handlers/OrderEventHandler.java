package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.coordination.eventProcessing.OrderEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.OrderRepository;

public abstract class OrderEventHandler extends EventHandler {
    private OrderRepository orderRepository;
    protected OrderEventProcessing orderEventProcessing;

    public OrderEventHandler(OrderRepository orderRepository, OrderEventProcessing orderEventProcessing) {
        this.orderRepository = orderRepository;
        this.orderEventProcessing = orderEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return orderRepository.findAll().stream().map(Order::getAggregateId).collect(Collectors.toSet());
    }

}
