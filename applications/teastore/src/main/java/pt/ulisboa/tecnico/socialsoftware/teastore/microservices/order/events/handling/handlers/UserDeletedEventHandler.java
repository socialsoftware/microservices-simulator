package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.OrderRepository;
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.eventProcessing.OrderEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.events.publish.UserDeletedEvent;

public class UserDeletedEventHandler extends OrderEventHandler {
    public UserDeletedEventHandler(OrderRepository orderRepository, OrderEventProcessing orderEventProcessing) {
        super(orderRepository, orderEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.orderEventProcessing.processUserDeletedEvent(subscriberAggregateId, (UserDeletedEvent) event);
    }
}
