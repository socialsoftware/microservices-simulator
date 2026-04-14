package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.discount.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.DiscountDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.webapi.requestDtos.CreateDiscountRequestDto;

public class CreateDiscountFunctionalitySagas extends WorkflowFunctionality {
    private DiscountDto createdDiscountDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateDiscountFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateDiscountRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateDiscountRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createDiscountStep = new SagaStep("createDiscountStep", () -> {
            CreateDiscountCommand cmd = new CreateDiscountCommand(unitOfWork, ServiceMapping.DISCOUNT.getServiceName(), createRequest);
            DiscountDto createdDiscountDto = (DiscountDto) commandGateway.send(cmd);
            setCreatedDiscountDto(createdDiscountDto);
        });

        workflow.addStep(createDiscountStep);
    }
    public DiscountDto getCreatedDiscountDto() {
        return createdDiscountDto;
    }

    public void setCreatedDiscountDto(DiscountDto createdDiscountDto) {
        this.createdDiscountDto = createdDiscountDto;
    }
}
