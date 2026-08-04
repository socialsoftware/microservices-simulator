package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.priceconfig.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdatePriceConfigFunctionalitySagas extends WorkflowFunctionality {
    private PriceConfigDto updatedPriceConfigDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdatePriceConfigFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, PriceConfigDto priceconfigDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(priceconfigDto, unitOfWork);
    }

    public void buildWorkflow(PriceConfigDto priceconfigDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updatePriceConfigStep = new SagaStep("updatePriceConfigStep", () -> {
            UpdatePriceConfigCommand cmd = new UpdatePriceConfigCommand(unitOfWork, ServiceMapping.PRICE_CONFIG.getServiceName(), priceconfigDto);
            PriceConfigDto updatedPriceConfigDto = (PriceConfigDto) commandGateway.send(cmd);
            setUpdatedPriceConfigDto(updatedPriceConfigDto);
        });

        workflow.addStep(updatePriceConfigStep);
    }
    public PriceConfigDto getUpdatedPriceConfigDto() {
        return updatedPriceConfigDto;
    }

    public void setUpdatedPriceConfigDto(PriceConfigDto updatedPriceConfigDto) {
        this.updatedPriceConfigDto = updatedPriceConfigDto;
    }
}
