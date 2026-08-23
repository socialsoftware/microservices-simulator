package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig.DeletePriceConfigCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig.GetPriceConfigByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.states.PriceConfigSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeletePriceConfigFunctionalitySagas extends WorkflowFunctionality {
    private PriceConfigDto priceConfigDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public DeletePriceConfigFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                               Integer priceConfigAggregateId,
                                               SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(priceConfigAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer priceConfigAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getPriceConfigStep = new SagaStep("getPriceConfigStep", () -> {
            GetPriceConfigByIdCommand readCmd = new GetPriceConfigByIdCommand(
                    unitOfWork, ServiceMapping.PRICE_CONFIG.getServiceName(), priceConfigAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(PriceConfigSagaState.IN_DELETE_PRICE_CONFIG);
            this.priceConfigDto = (PriceConfigDto) commandGateway.send(sagaCommand);
        });

        SagaStep deletePriceConfigStep = new SagaStep("deletePriceConfigStep", () -> {
            DeletePriceConfigCommand cmd = new DeletePriceConfigCommand(
                    unitOfWork, ServiceMapping.PRICE_CONFIG.getServiceName(), priceConfigAggregateId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getPriceConfigStep)));

        this.workflow.addStep(getPriceConfigStep);
        this.workflow.addStep(deletePriceConfigStep);
    }

    public PriceConfigDto getPriceConfigDto() {
        return priceConfigDto;
    }
}
