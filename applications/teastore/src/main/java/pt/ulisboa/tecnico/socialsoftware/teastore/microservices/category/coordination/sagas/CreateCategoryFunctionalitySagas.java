package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.teastore.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.teastore.command.category.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.coordination.webapi.requestDtos.CreateCategoryRequestDto;

public class CreateCategoryFunctionalitySagas extends WorkflowFunctionality {
    private CategoryDto createdCategoryDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateCategoryFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateCategoryRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateCategoryRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createCategoryStep = new SagaStep("createCategoryStep", () -> {
            CreateCategoryCommand cmd = new CreateCategoryCommand(unitOfWork, ServiceMapping.CATEGORY.getServiceName(), createRequest);
            CategoryDto createdCategoryDto = (CategoryDto) commandGateway.send(cmd);
            setCreatedCategoryDto(createdCategoryDto);
        });

        workflow.addStep(createCategoryStep);
    }
    public CategoryDto getCreatedCategoryDto() {
        return createdCategoryDto;
    }

    public void setCreatedCategoryDto(CategoryDto createdCategoryDto) {
        this.createdCategoryDto = createdCategoryDto;
    }
}
