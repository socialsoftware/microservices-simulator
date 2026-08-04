package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.eventProcessing.ContactsEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.Contacts;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsRepository;

public abstract class ContactsEventHandler extends EventHandler {
    private ContactsRepository contactsRepository;
    protected ContactsEventProcessing contactsEventProcessing;

    public ContactsEventHandler(ContactsRepository contactsRepository, ContactsEventProcessing contactsEventProcessing) {
        this.contactsRepository = contactsRepository;
        this.contactsEventProcessing = contactsEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return contactsRepository.findAll().stream().map(Contacts::getAggregateId).collect(Collectors.toSet());
    }

}
