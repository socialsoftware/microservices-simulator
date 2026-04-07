package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.cart.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.webapi.requestDtos.CreateCartRequestDto;

public class CreateCartFunctionalitySagas extends WorkflowFunctionality {
    private CartDto createdCartDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateCartFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateCartRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateCartRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createCartStep = new SagaStep("createCartStep", () -> {
            CreateCartCommand cmd = new CreateCartCommand(unitOfWork, ServiceMapping.CART.getServiceName(), createRequest);
            CartDto createdCartDto = (CartDto) commandGateway.send(cmd);
            setCreatedCartDto(createdCartDto);
        });

        workflow.addStep(createCartStep);
    }
    public CartDto getCreatedCartDto() {
        return createdCartDto;
    }

    public void setCreatedCartDto(CartDto createdCartDto) {
        this.createdCartDto = createdCartDto;
    }
}
