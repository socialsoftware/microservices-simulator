package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig.GetPriceConfigsCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto;

import java.util.List;

public class GetPriceConfigsFunctionalitySagas extends WorkflowFunctionality {
    private List<PriceConfigDto> priceConfigs;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetPriceConfigsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                             SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(unitOfWork);
    }

    @SuppressWarnings("unchecked")
    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getPriceConfigsStep = new SagaStep("getPriceConfigsStep", () -> {
            GetPriceConfigsCommand cmd = new GetPriceConfigsCommand(
                    unitOfWork, ServiceMapping.PRICE_CONFIG.getServiceName());
            this.priceConfigs = (List<PriceConfigDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getPriceConfigsStep);
    }

    public List<PriceConfigDto> getPriceConfigs() {
        return priceConfigs;
    }
}
