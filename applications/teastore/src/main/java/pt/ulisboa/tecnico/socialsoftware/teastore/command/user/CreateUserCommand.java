package pt.ulisboa.tecnico.socialsoftware.teastore.command.user;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto;

public class CreateUserCommand extends Command {
    private final CreateUserRequestDto createRequest;

    public CreateUserCommand(UnitOfWork unitOfWork, String serviceName, CreateUserRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateUserRequestDto getCreateRequest() { return createRequest; }
}
