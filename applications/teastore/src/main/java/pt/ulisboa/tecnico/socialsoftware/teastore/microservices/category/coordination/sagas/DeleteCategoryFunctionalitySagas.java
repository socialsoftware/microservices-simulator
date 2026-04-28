package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.teastore.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.teastore.command.category.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.sagas.states.CategorySagaState;

public class DeleteCategoryFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteCategoryFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer categoryAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(categoryAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer categoryAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteCategoryStep = new SagaStep("deleteCategoryStep", () -> {
            unitOfWorkService.verifySagaState(categoryAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(CategorySagaState.READ_CATEGORY, CategorySagaState.UPDATE_CATEGORY, CategorySagaState.DELETE_CATEGORY)));
            unitOfWorkService.registerSagaState(categoryAggregateId, CategorySagaState.DELETE_CATEGORY, unitOfWork);
            DeleteCategoryCommand cmd = new DeleteCategoryCommand(unitOfWork, ServiceMapping.CATEGORY.getServiceName(), categoryAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteCategoryStep);
    }
}
