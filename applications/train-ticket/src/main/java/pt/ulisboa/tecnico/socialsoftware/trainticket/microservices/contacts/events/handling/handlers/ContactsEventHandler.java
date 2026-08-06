package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.eventProcessing.ContactsEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsRepository;

public abstract class ContactsEventHandler extends EventHandler {
    protected ContactsEventProcessing contactsEventProcessing;

    public ContactsEventHandler(ContactsRepository contactsRepository, ContactsEventProcessing contactsEventProcessing) {
        super(contactsRepository);
        this.contactsEventProcessing = contactsEventProcessing;
    }

}
