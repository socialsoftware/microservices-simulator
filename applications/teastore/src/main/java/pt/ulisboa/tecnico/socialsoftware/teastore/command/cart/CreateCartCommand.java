package pt.ulisboa.tecnico.socialsoftware.teastore.command.cart;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.coordination.webapi.requestDtos.CreateCartRequestDto;

public class CreateCartCommand extends Command {
    private final CreateCartRequestDto createRequest;

    public CreateCartCommand(UnitOfWork unitOfWork, String serviceName, CreateCartRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateCartRequestDto getCreateRequest() { return createRequest; }
}
