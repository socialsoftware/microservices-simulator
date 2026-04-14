package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.shipping.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ShippingDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.webapi.requestDtos.CreateShippingRequestDto;

public class CreateShippingFunctionalitySagas extends WorkflowFunctionality {
    private ShippingDto createdShippingDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateShippingFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateShippingRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateShippingRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createShippingStep = new SagaStep("createShippingStep", () -> {
            CreateShippingCommand cmd = new CreateShippingCommand(unitOfWork, ServiceMapping.SHIPPING.getServiceName(), createRequest);
            ShippingDto createdShippingDto = (ShippingDto) commandGateway.send(cmd);
            setCreatedShippingDto(createdShippingDto);
        });

        workflow.addStep(createShippingStep);
    }
    public ShippingDto getCreatedShippingDto() {
        return createdShippingDto;
    }

    public void setCreatedShippingDto(ShippingDto createdShippingDto) {
        this.createdShippingDto = createdShippingDto;
    }
}
