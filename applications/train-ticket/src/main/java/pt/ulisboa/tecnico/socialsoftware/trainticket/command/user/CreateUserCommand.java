package pt.ulisboa.tecnico.socialsoftware.trainticket.command.user;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto;

public class CreateUserCommand extends Command {
    private final CreateUserRequestDto createRequest;

    public CreateUserCommand(UnitOfWork unitOfWork, String serviceName, CreateUserRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateUserRequestDto getCreateRequest() { return createRequest; }
}
