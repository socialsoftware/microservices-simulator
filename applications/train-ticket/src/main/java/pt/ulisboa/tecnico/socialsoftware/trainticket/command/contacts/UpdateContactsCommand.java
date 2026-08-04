package pt.ulisboa.tecnico.socialsoftware.trainticket.command.contacts;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.ContactsDto;

public class UpdateContactsCommand extends Command {
    private final ContactsDto contactsDto;

    public UpdateContactsCommand(UnitOfWork unitOfWork, String serviceName, ContactsDto contactsDto) {
        super(unitOfWork, serviceName, null);
        this.contactsDto = contactsDto;
    }

    public ContactsDto getContactsDto() { return contactsDto; }
}
