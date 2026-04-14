package pt.ulisboa.tecnico.socialsoftware.ecommerce.command.discount;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.webapi.requestDtos.CreateDiscountRequestDto;

public class CreateDiscountCommand extends Command {
    private final CreateDiscountRequestDto createRequest;

    public CreateDiscountCommand(UnitOfWork unitOfWork, String serviceName, CreateDiscountRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateDiscountRequestDto getCreateRequest() { return createRequest; }
}
