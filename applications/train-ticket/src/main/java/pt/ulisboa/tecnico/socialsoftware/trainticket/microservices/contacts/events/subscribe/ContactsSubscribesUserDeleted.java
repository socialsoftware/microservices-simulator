package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.Contacts;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.UserDeletedEvent;

public class ContactsSubscribesUserDeleted extends EventSubscription {
    public ContactsSubscribesUserDeleted(Contacts contacts) {
        super(contacts.getAggregateId(), 0, UserDeletedEvent.class.getSimpleName());
    }
}
