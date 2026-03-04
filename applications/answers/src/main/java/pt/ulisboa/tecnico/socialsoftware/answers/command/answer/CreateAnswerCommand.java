package pt.ulisboa.tecnico.socialsoftware.answers.command.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.webapi.requestDtos.CreateAnswerRequestDto;

public class CreateAnswerCommand extends Command {
    private final CreateAnswerRequestDto createRequest;

    public CreateAnswerCommand(UnitOfWork unitOfWork, String serviceName, CreateAnswerRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateAnswerRequestDto getCreateRequest() { return createRequest; }
}
