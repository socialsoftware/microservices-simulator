package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.priceconfig.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeletePriceConfigFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeletePriceConfigFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer priceconfigAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(priceconfigAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer priceconfigAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deletePriceConfigStep = new SagaStep("deletePriceConfigStep", () -> {
            DeletePriceConfigCommand cmd = new DeletePriceConfigCommand(unitOfWork, ServiceMapping.PRICE_CONFIG.getServiceName(), priceconfigAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deletePriceConfigStep);
    }
}
