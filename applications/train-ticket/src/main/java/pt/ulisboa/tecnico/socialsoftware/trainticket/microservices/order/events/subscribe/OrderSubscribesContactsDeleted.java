package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.ContactsDeletedEvent;

public class OrderSubscribesContactsDeleted extends EventSubscription {
    public OrderSubscribesContactsDeleted(Order order) {
        super(order.getAggregateId(), 0, ContactsDeletedEvent.class.getSimpleName());
    }
}
