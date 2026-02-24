package pt.ulisboa.tecnico.socialsoftware.teastore.command.product;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.coordination.webapi.requestDtos.CreateProductRequestDto;

public class CreateProductCommand extends Command {
    private final CreateProductRequestDto createRequest;

    public CreateProductCommand(UnitOfWork unitOfWork, String serviceName, CreateProductRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateProductRequestDto getCreateRequest() { return createRequest; }
}
