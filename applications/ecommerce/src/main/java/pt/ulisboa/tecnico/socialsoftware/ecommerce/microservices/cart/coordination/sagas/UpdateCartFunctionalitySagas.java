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

public class UpdateCartFunctionalitySagas extends WorkflowFunctionality {
    private CartDto updatedCartDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateCartFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CartDto cartDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(cartDto, unitOfWork);
    }

    public void buildWorkflow(CartDto cartDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateCartStep = new SagaStep("updateCartStep", () -> {
            UpdateCartCommand cmd = new UpdateCartCommand(unitOfWork, ServiceMapping.CART.getServiceName(), cartDto);
            CartDto updatedCartDto = (CartDto) commandGateway.send(cmd);
            setUpdatedCartDto(updatedCartDto);
        });

        workflow.addStep(updateCartStep);
    }
    public CartDto getUpdatedCartDto() {
        return updatedCartDto;
    }

    public void setUpdatedCartDto(CartDto updatedCartDto) {
        this.updatedCartDto = updatedCartDto;
    }
}
