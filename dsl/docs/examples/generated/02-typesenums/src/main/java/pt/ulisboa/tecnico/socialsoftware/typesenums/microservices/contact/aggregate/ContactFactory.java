package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate;

import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos.ContactDto;

public interface ContactFactory {
    Contact createContact(Integer aggregateId, ContactDto contactDto);
    Contact createContactFromExisting(Contact existingContact);
    ContactDto createContactDto(Contact contact);
}
