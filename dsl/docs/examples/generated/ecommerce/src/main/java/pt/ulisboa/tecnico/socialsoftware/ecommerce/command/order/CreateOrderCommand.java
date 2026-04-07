package pt.ulisboa.tecnico.socialsoftware.ecommerce.command.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.coordination.webapi.requestDtos.CreateOrderRequestDto;

public class CreateOrderCommand extends Command {
    private final CreateOrderRequestDto createRequest;

    public CreateOrderCommand(UnitOfWork unitOfWork, String serviceName, CreateOrderRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateOrderRequestDto getCreateRequest() { return createRequest; }
}
