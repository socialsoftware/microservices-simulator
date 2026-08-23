package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.Contacts;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.SagaContacts;

@Service
@Profile("sagas")
public class SagasContactsFactory implements ContactsFactory {
    @Override
    public SagaContacts createContacts(Integer aggregateId, ContactsDto contactsDto) {
        return new SagaContacts(aggregateId, contactsDto);
    }

    @Override
    public SagaContacts createContactsCopy(Contacts existing) {
        return new SagaContacts((SagaContacts) existing);
    }

    @Override
    public ContactsDto createContactsDto(Contacts contacts) {
        return new ContactsDto(contacts);
    }
}
