package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.coordination.eventProcessing.OrderEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.OrderRepository;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.UserUpdatedEvent;

public class UserUpdatedEventHandler extends OrderEventHandler {
    public UserUpdatedEventHandler(OrderRepository orderRepository, OrderEventProcessing orderEventProcessing) {
        super(orderRepository, orderEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.orderEventProcessing.processUserUpdatedEvent(subscriberAggregateId, (UserUpdatedEvent) event);
    }
}
