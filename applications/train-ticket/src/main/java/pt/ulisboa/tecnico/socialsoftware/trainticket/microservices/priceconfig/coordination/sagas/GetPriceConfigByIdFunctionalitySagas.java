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

public class GetPriceConfigByIdFunctionalitySagas extends WorkflowFunctionality {
    private PriceConfigDto priceconfigDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetPriceConfigByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer priceconfigAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(priceconfigAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer priceconfigAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getPriceConfigStep = new SagaStep("getPriceConfigStep", () -> {
            GetPriceConfigByIdCommand cmd = new GetPriceConfigByIdCommand(unitOfWork, ServiceMapping.PRICE_CONFIG.getServiceName(), priceconfigAggregateId);
            PriceConfigDto priceconfigDto = (PriceConfigDto) commandGateway.send(cmd);
            setPriceConfigDto(priceconfigDto);
        });

        workflow.addStep(getPriceConfigStep);
    }
    public PriceConfigDto getPriceConfigDto() {
        return priceconfigDto;
    }

    public void setPriceConfigDto(PriceConfigDto priceconfigDto) {
        this.priceconfigDto = priceconfigDto;
    }
}
