package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderContacts;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.ContactsDeletedEvent;


public class OrderSubscribesContactsDeletedOrderContactsExists extends EventSubscription {
    public OrderSubscribesContactsDeletedOrderContactsExists(OrderContacts contacts) {
        super(contacts.getContactsAggregateId(),
                contacts.getContactsVersion(),
                ContactsDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
