package pt.ulisboa.tecnico.socialsoftware.helloworld.command.task;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.coordination.webapi.requestDtos.CreateTaskRequestDto;

public class CreateTaskCommand extends Command {
    private final CreateTaskRequestDto createRequest;

    public CreateTaskCommand(UnitOfWork unitOfWork, String serviceName, CreateTaskRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateTaskRequestDto getCreateRequest() { return createRequest; }
}
