package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate;

public interface ContactsFactory {
    Contacts createContacts(Integer aggregateId, ContactsDto contactsDto);

    Contacts createContactsCopy(Contacts existing);

    ContactsDto createContactsDto(Contacts contacts);
}
