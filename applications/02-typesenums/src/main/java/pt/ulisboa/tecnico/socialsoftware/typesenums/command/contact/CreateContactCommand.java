package pt.ulisboa.tecnico.socialsoftware.typesenums.command.contact;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.coordination.webapi.requestDtos.CreateContactRequestDto;

public class CreateContactCommand extends Command {
    private final CreateContactRequestDto createRequest;

    public CreateContactCommand(UnitOfWork unitOfWork, String serviceName, CreateContactRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateContactRequestDto getCreateRequest() { return createRequest; }
}
