package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto;

public class CreateContactsCommand extends Command {
    private ContactsDto contactsDto;

    public CreateContactsCommand(UnitOfWork unitOfWork, String serviceName, ContactsDto contactsDto) {
        super(unitOfWork, serviceName, null);
        this.contactsDto = contactsDto;
    }

    public ContactsDto getContactsDto() {
        return contactsDto;
    }
}
