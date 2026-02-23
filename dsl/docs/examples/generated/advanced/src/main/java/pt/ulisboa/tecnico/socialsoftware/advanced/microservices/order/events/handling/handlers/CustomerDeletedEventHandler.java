package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.OrderRepository;
import pt.ulisboa.tecnico.socialsoftware.advanced.coordination.eventProcessing.OrderEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.events.publish.CustomerDeletedEvent;

public class CustomerDeletedEventHandler extends OrderEventHandler {
    public CustomerDeletedEventHandler(OrderRepository orderRepository, OrderEventProcessing orderEventProcessing) {
        super(orderRepository, orderEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.orderEventProcessing.processCustomerDeletedEvent(subscriberAggregateId, (CustomerDeletedEvent) event);
    }
}
