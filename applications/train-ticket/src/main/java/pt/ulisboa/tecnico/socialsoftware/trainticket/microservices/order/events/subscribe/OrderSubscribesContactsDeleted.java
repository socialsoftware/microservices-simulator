package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.ContactsDeletedEvent;

public class OrderSubscribesContactsDeleted extends EventSubscription {
    public OrderSubscribesContactsDeleted(Order order) {
        super(order.getAggregateId(), 0L, ContactsDeletedEvent.class.getSimpleName());
    }
}
