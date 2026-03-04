package pt.ulisboa.tecnico.socialsoftware.eventdriven.command.author;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.coordination.webapi.requestDtos.CreateAuthorRequestDto;

public class CreateAuthorCommand extends Command {
    private final CreateAuthorRequestDto createRequest;

    public CreateAuthorCommand(UnitOfWork unitOfWork, String serviceName, CreateAuthorRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateAuthorRequestDto getCreateRequest() { return createRequest; }
}
