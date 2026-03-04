package pt.ulisboa.tecnico.socialsoftware.crossrefs.command.enrollment;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.webapi.requestDtos.CreateEnrollmentRequestDto;

public class CreateEnrollmentCommand extends Command {
    private final CreateEnrollmentRequestDto createRequest;

    public CreateEnrollmentCommand(UnitOfWork unitOfWork, String serviceName, CreateEnrollmentRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateEnrollmentRequestDto getCreateRequest() { return createRequest; }
}
