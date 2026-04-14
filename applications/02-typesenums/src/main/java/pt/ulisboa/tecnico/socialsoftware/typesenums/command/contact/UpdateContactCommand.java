package pt.ulisboa.tecnico.socialsoftware.typesenums.command.contact;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos.ContactDto;

public class UpdateContactCommand extends Command {
    private final ContactDto contactDto;

    public UpdateContactCommand(UnitOfWork unitOfWork, String serviceName, ContactDto contactDto) {
        super(unitOfWork, serviceName, null);
        this.contactDto = contactDto;
    }

    public ContactDto getContactDto() { return contactDto; }
}
