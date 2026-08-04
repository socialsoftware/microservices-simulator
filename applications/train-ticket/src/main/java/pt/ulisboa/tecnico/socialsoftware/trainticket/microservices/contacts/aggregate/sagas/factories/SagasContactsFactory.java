package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.Contacts;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.SagaContacts;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.dtos.SagaContactsDto;

@Service
@Profile("sagas")
public class SagasContactsFactory implements ContactsFactory {
    @Override
    public Contacts createContacts(Integer aggregateId, ContactsDto contactsDto) {
        return new SagaContacts(aggregateId, contactsDto);
    }

    @Override
    public Contacts createContactsFromExisting(Contacts existingContacts) {
        return new SagaContacts((SagaContacts) existingContacts);
    }

    @Override
    public ContactsDto createContactsDto(Contacts contacts) {
        return new SagaContactsDto(contacts);
    }
}