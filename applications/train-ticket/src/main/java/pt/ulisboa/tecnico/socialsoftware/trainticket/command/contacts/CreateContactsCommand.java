package pt.ulisboa.tecnico.socialsoftware.trainticket.command.contacts;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.webapi.requestDtos.CreateContactsRequestDto;

public class CreateContactsCommand extends Command {
    private final CreateContactsRequestDto createRequest;

    public CreateContactsCommand(UnitOfWork unitOfWork, String serviceName, CreateContactsRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateContactsRequestDto getCreateRequest() { return createRequest; }
}
