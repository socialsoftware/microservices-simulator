package pt.ulisboa.tecnico.socialsoftware.answers.command.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.webapi.requestDtos.CreateExecutionRequestDto;

public class CreateExecutionCommand extends Command {
    private final CreateExecutionRequestDto createRequest;

    public CreateExecutionCommand(UnitOfWork unitOfWork, String serviceName, CreateExecutionRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateExecutionRequestDto getCreateRequest() { return createRequest; }
}
