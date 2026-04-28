package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.teastore.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.teastore.command.category.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.sagas.states.CategorySagaState;

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
            unitOfWorkService.verifySagaState(categoryDto.getAggregateId(), new java.util.ArrayList<SagaState>(java.util.Arrays.asList(CategorySagaState.READ_CATEGORY, CategorySagaState.UPDATE_CATEGORY, CategorySagaState.DELETE_CATEGORY)));
            unitOfWorkService.registerSagaState(categoryDto.getAggregateId(), CategorySagaState.UPDATE_CATEGORY, unitOfWork);
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
