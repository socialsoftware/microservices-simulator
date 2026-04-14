package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.discount.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteDiscountFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteDiscountFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer discountAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(discountAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer discountAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteDiscountStep = new SagaStep("deleteDiscountStep", () -> {
            DeleteDiscountCommand cmd = new DeleteDiscountCommand(unitOfWork, ServiceMapping.DISCOUNT.getServiceName(), discountAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteDiscountStep);
    }
}
