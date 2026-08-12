package pt.ulisboa.tecnico.socialsoftware.answers.command.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.webapi.requestDtos.CreateTopicRequestDto;

public class CreateTopicCommand extends Command {
    private final CreateTopicRequestDto createRequest;

    public CreateTopicCommand(UnitOfWork unitOfWork, String serviceName, CreateTopicRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateTopicRequestDto getCreateRequest() { return createRequest; }
}
