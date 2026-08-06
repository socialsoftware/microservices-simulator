package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsUser;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.UserDeletedEvent;


public class ContactsSubscribesUserDeletedContactsUserExists extends EventSubscription {
    public ContactsSubscribesUserDeletedContactsUserExists(ContactsUser user) {
        super(user.getUserAggregateId(),
                user.getUserVersion(),
                UserDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
