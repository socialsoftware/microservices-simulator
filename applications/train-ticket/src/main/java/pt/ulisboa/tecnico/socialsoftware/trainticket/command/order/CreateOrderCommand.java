package pt.ulisboa.tecnico.socialsoftware.trainticket.command.order;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.webapi.requestDtos.CreateOrderRequestDto;

public class CreateOrderCommand extends Command {
    private final CreateOrderRequestDto createRequest;

    public CreateOrderCommand(UnitOfWork unitOfWork, String serviceName, CreateOrderRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateOrderRequestDto getCreateRequest() { return createRequest; }
}
