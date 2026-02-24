package pt.ulisboa.tecnico.socialsoftware.answers.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.webapi.requestDtos.CreateQuizRequestDto;

public class CreateQuizCommand extends Command {
    private final CreateQuizRequestDto createRequest;

    public CreateQuizCommand(UnitOfWork unitOfWork, String serviceName, CreateQuizRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateQuizRequestDto getCreateRequest() { return createRequest; }
}
