package pt.ulisboa.tecnico.socialsoftware.advanced.command.customer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.coordination.webapi.requestDtos.CreateCustomerRequestDto;

public class CreateCustomerCommand extends Command {
    private final CreateCustomerRequestDto createRequest;

    public CreateCustomerCommand(UnitOfWork unitOfWork, String serviceName, CreateCustomerRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateCustomerRequestDto getCreateRequest() { return createRequest; }
}
