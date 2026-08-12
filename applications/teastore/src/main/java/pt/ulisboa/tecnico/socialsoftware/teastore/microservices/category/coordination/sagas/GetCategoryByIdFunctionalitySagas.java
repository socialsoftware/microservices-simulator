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

public class GetCategoryByIdFunctionalitySagas extends WorkflowFunctionality {
    private CategoryDto categoryDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetCategoryByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer categoryAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(categoryAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer categoryAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getCategoryStep = new SagaStep("getCategoryStep", () -> {
            GetCategoryByIdCommand cmd = new GetCategoryByIdCommand(unitOfWork, ServiceMapping.CATEGORY.getServiceName(), categoryAggregateId);
            CategoryDto categoryDto = (CategoryDto) commandGateway.send(cmd);
            setCategoryDto(categoryDto);
        });

        workflow.addStep(getCategoryStep);
    }
    public CategoryDto getCategoryDto() {
        return categoryDto;
    }

    public void setCategoryDto(CategoryDto categoryDto) {
        this.categoryDto = categoryDto;
    }
}
