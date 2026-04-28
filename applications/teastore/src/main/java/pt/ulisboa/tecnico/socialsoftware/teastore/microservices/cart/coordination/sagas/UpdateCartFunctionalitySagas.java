package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.teastore.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.teastore.command.cart.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.sagas.states.CartSagaState;

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
            unitOfWorkService.verifySagaState(cartDto.getAggregateId(), new java.util.ArrayList<SagaState>(java.util.Arrays.asList(CartSagaState.READ_CART, CartSagaState.UPDATE_CART, CartSagaState.DELETE_CART)));
            unitOfWorkService.registerSagaState(cartDto.getAggregateId(), CartSagaState.UPDATE_CART, unitOfWork);
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
