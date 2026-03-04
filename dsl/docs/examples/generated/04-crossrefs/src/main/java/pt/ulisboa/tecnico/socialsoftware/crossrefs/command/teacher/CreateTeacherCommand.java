package pt.ulisboa.tecnico.socialsoftware.crossrefs.command.teacher;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.coordination.webapi.requestDtos.CreateTeacherRequestDto;

public class CreateTeacherCommand extends Command {
    private final CreateTeacherRequestDto createRequest;

    public CreateTeacherCommand(UnitOfWork unitOfWork, String serviceName, CreateTeacherRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateTeacherRequestDto getCreateRequest() { return createRequest; }
}
