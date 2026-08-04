package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.ContactsDto;

public interface ContactsFactory {
    Contacts createContacts(Integer aggregateId, ContactsDto contactsDto);
    Contacts createContactsFromExisting(Contacts existingContacts);
    ContactsDto createContactsDto(Contacts contacts);
}
