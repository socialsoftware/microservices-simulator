package pt.ulisboa.tecnico.socialsoftware.tutorial.command.book;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.coordination.webapi.requestDtos.CreateBookRequestDto;

public class CreateBookCommand extends Command {
    private final CreateBookRequestDto createRequest;

    public CreateBookCommand(UnitOfWork unitOfWork, String serviceName, CreateBookRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateBookRequestDto getCreateRequest() { return createRequest; }
}
