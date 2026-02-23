package pt.ulisboa.tecnico.socialsoftware.typesenums.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.Contact;
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos.ContactDto;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.ContactFactory;
import pt.ulisboa.tecnico.socialsoftware.typesenums.sagas.aggregates.SagaContact;
import pt.ulisboa.tecnico.socialsoftware.typesenums.sagas.aggregates.dtos.SagaContactDto;

@Service
@Profile("sagas")
public class SagasContactFactory implements ContactFactory {
    @Override
    public Contact createContact(Integer aggregateId, ContactDto contactDto) {
        return new SagaContact(aggregateId, contactDto);
    }

    @Override
    public Contact createContactFromExisting(Contact existingContact) {
        return new SagaContact((SagaContact) existingContact);
    }

    @Override
    public ContactDto createContactDto(Contact contact) {
        return new SagaContactDto(contact);
    }
}