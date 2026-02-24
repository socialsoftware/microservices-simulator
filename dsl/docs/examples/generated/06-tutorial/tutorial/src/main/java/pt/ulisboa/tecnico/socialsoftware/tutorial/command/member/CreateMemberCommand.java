package pt.ulisboa.tecnico.socialsoftware.tutorial.command.member;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.coordination.webapi.requestDtos.CreateMemberRequestDto;

public class CreateMemberCommand extends Command {
    private final CreateMemberRequestDto createRequest;

    public CreateMemberCommand(UnitOfWork unitOfWork, String serviceName, CreateMemberRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateMemberRequestDto getCreateRequest() { return createRequest; }
}
