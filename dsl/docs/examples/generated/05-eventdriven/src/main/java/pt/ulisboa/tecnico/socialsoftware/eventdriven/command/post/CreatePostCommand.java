package pt.ulisboa.tecnico.socialsoftware.eventdriven.command.post;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.coordination.webapi.requestDtos.CreatePostRequestDto;

public class CreatePostCommand extends Command {
    private final CreatePostRequestDto createRequest;

    public CreatePostCommand(UnitOfWork unitOfWork, String serviceName, CreatePostRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreatePostRequestDto getCreateRequest() { return createRequest; }
}
