package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.eventProcessing.OrderEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderRepository;

public abstract class OrderEventHandler extends EventHandler {
    protected OrderEventProcessing orderEventProcessing;

    public OrderEventHandler(OrderRepository orderRepository, OrderEventProcessing orderEventProcessing) {
        super(orderRepository);
        this.orderEventProcessing = orderEventProcessing;
    }

}
