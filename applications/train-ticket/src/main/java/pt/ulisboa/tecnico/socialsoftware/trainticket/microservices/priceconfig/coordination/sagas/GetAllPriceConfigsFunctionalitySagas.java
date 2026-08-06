package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.priceconfig.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllPriceConfigsFunctionalitySagas extends WorkflowFunctionality {
    private List<PriceConfigDto> priceconfigs;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllPriceConfigsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllPriceConfigsStep = new SagaStep("getAllPriceConfigsStep", () -> {
            GetAllPriceConfigsCommand cmd = new GetAllPriceConfigsCommand(unitOfWork, ServiceMapping.PRICE_CONFIG.getServiceName());
            List<PriceConfigDto> priceconfigs = (List<PriceConfigDto>) commandGateway.send(cmd);
            setPriceConfigs(priceconfigs);
        });

        workflow.addStep(getAllPriceConfigsStep);
    }
    public List<PriceConfigDto> getPriceConfigs() {
        return priceconfigs;
    }

    public void setPriceConfigs(List<PriceConfigDto> priceconfigs) {
        this.priceconfigs = priceconfigs;
    }
}
