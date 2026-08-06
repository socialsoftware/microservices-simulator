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
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.webapi.requestDtos.CreatePriceConfigRequestDto;

public class CreatePriceConfigFunctionalitySagas extends WorkflowFunctionality {
    private PriceConfigDto createdPriceConfigDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreatePriceConfigFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreatePriceConfigRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreatePriceConfigRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createPriceConfigStep = new SagaStep("createPriceConfigStep", () -> {
            CreatePriceConfigCommand cmd = new CreatePriceConfigCommand(unitOfWork, ServiceMapping.PRICE_CONFIG.getServiceName(), createRequest);
            PriceConfigDto createdPriceConfigDto = (PriceConfigDto) commandGateway.send(cmd);
            setCreatedPriceConfigDto(createdPriceConfigDto);
        });

        workflow.addStep(createPriceConfigStep);
    }
    public PriceConfigDto getCreatedPriceConfigDto() {
        return createdPriceConfigDto;
    }

    public void setCreatedPriceConfigDto(PriceConfigDto createdPriceConfigDto) {
        this.createdPriceConfigDto = createdPriceConfigDto;
    }
}
