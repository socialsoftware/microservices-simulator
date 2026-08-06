package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.eventProcessing.OrderEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.ContactsDeletedEvent;

public class ContactsDeletedEventHandler extends OrderEventHandler {
    public ContactsDeletedEventHandler(OrderRepository orderRepository, OrderEventProcessing orderEventProcessing) {
        super(orderRepository, orderEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.orderEventProcessing.processContactsDeletedEvent(subscriberAggregateId, (ContactsDeletedEvent) event);
    }
}
