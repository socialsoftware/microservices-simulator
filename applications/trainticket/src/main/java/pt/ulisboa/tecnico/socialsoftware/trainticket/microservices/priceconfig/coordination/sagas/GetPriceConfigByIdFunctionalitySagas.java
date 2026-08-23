package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig.GetPriceConfigByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto;

public class GetPriceConfigByIdFunctionalitySagas extends WorkflowFunctionality {
    private PriceConfigDto priceConfigDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetPriceConfigByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                Integer priceConfigAggregateId,
                                                SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(priceConfigAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer priceConfigAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getPriceConfigStep = new SagaStep("getPriceConfigStep", () -> {
            GetPriceConfigByIdCommand cmd = new GetPriceConfigByIdCommand(
                    unitOfWork, ServiceMapping.PRICE_CONFIG.getServiceName(), priceConfigAggregateId);
            this.priceConfigDto = (PriceConfigDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(getPriceConfigStep);
    }

    public PriceConfigDto getPriceConfigDto() {
        return priceConfigDto;
    }
}
