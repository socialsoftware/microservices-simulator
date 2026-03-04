package pt.ulisboa.tecnico.socialsoftware.teastore.command.category;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.coordination.webapi.requestDtos.CreateCategoryRequestDto;

public class CreateCategoryCommand extends Command {
    private final CreateCategoryRequestDto createRequest;

    public CreateCategoryCommand(UnitOfWork unitOfWork, String serviceName, CreateCategoryRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateCategoryRequestDto getCreateRequest() { return createRequest; }
}
