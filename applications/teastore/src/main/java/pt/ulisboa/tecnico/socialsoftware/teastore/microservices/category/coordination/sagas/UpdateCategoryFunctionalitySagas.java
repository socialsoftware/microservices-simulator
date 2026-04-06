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

public class UpdateCategoryFunctionalitySagas extends WorkflowFunctionality {
    private CategoryDto updatedCategoryDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateCategoryFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CategoryDto categoryDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(categoryDto, unitOfWork);
    }

    public void buildWorkflow(CategoryDto categoryDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateCategoryStep = new SagaStep("updateCategoryStep", () -> {
            UpdateCategoryCommand cmd = new UpdateCategoryCommand(unitOfWork, ServiceMapping.CATEGORY.getServiceName(), categoryDto);
            CategoryDto updatedCategoryDto = (CategoryDto) commandGateway.send(cmd);
            setUpdatedCategoryDto(updatedCategoryDto);
        });

        workflow.addStep(updateCategoryStep);
    }
    public CategoryDto getUpdatedCategoryDto() {
        return updatedCategoryDto;
    }

    public void setUpdatedCategoryDto(CategoryDto updatedCategoryDto) {
        this.updatedCategoryDto = updatedCategoryDto;
    }
}
