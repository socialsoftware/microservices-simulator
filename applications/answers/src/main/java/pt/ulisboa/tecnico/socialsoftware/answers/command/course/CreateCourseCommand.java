package pt.ulisboa.tecnico.socialsoftware.answers.command.course;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.webapi.requestDtos.CreateCourseRequestDto;

public class CreateCourseCommand extends Command {
    private final CreateCourseRequestDto createRequest;

    public CreateCourseCommand(UnitOfWork unitOfWork, String serviceName, CreateCourseRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateCourseRequestDto getCreateRequest() { return createRequest; }
}
