package pt.ulisboa.tecnico.socialsoftware.teastore.command.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.coordination.webapi.requestDtos.CreateOrderRequestDto;

public class CreateOrderCommand extends Command {
    private final CreateOrderRequestDto createRequest;

    public CreateOrderCommand(UnitOfWork unitOfWork, String serviceName, CreateOrderRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateOrderRequestDto getCreateRequest() { return createRequest; }
}
