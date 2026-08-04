package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.eventProcessing.ContactsEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.UserDeletedEvent;

public class UserDeletedEventHandler extends ContactsEventHandler {
    public UserDeletedEventHandler(ContactsRepository contactsRepository, ContactsEventProcessing contactsEventProcessing) {
        super(contactsRepository, contactsEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.contactsEventProcessing.processUserDeletedEvent(subscriberAggregateId, (UserDeletedEvent) event);
    }
}
