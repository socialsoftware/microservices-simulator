package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig.GetPriceConfigByRouteAndTrainTypeCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto;

public class GetPriceConfigByRouteAndTrainTypeFunctionalitySagas extends WorkflowFunctionality {
    private PriceConfigDto priceConfigDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetPriceConfigByRouteAndTrainTypeFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                               Integer routeAggregateId,
                                                               Integer trainTypeAggregateId,
                                                               SagaUnitOfWork unitOfWork,
                                                               CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(routeAggregateId, trainTypeAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer routeAggregateId, Integer trainTypeAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getPriceConfigStep = new SagaStep("getPriceConfigStep", () -> {
            GetPriceConfigByRouteAndTrainTypeCommand cmd = new GetPriceConfigByRouteAndTrainTypeCommand(
                    unitOfWork, ServiceMapping.PRICE_CONFIG.getServiceName(),
                    routeAggregateId, trainTypeAggregateId);
            this.priceConfigDto = (PriceConfigDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(getPriceConfigStep);
    }

    public PriceConfigDto getPriceConfigDto() {
        return priceConfigDto;
    }
}
