package pt.ulisboa.tecnico.socialsoftware.ecommerce.command.shipping;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.webapi.requestDtos.CreateShippingRequestDto;

public class CreateShippingCommand extends Command {
    private final CreateShippingRequestDto createRequest;

    public CreateShippingCommand(UnitOfWork unitOfWork, String serviceName, CreateShippingRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateShippingRequestDto getCreateRequest() { return createRequest; }
}
