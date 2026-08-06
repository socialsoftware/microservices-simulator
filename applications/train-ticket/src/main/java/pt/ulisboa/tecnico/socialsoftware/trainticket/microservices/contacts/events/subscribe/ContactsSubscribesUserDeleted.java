package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.Contacts;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.UserDeletedEvent;

public class ContactsSubscribesUserDeleted extends EventSubscription {
    public ContactsSubscribesUserDeleted(Contacts contacts) {
        super(contacts.getAggregateId(), 0L, UserDeletedEvent.class.getSimpleName());
    }
}
