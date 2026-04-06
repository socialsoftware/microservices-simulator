package pt.ulisboa.tecnico.socialsoftware.answers.command.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.webapi.requestDtos.CreateQuestionRequestDto;

public class CreateQuestionCommand extends Command {
    private final CreateQuestionRequestDto createRequest;

    public CreateQuestionCommand(UnitOfWork unitOfWork, String serviceName, CreateQuestionRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateQuestionRequestDto getCreateRequest() { return createRequest; }
}
