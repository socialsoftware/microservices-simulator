package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.order.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderItemDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateOrderItemFunctionalitySagas extends WorkflowFunctionality {
    private OrderItemDto updatedItemDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateOrderItemFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer orderId, Integer key, OrderItemDto itemDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(orderId, key, itemDto, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, Integer key, OrderItemDto itemDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateItemStep = new SagaStep("updateItemStep", () -> {
            UpdateOrderItemCommand cmd = new UpdateOrderItemCommand(unitOfWork, ServiceMapping.ORDER.getServiceName(), orderId, key, itemDto);
            OrderItemDto updatedItemDto = (OrderItemDto) commandGateway.send(cmd);
            setUpdatedItemDto(updatedItemDto);
        });

        workflow.addStep(updateItemStep);
    }
    public OrderItemDto getUpdatedItemDto() {
        return updatedItemDto;
    }

    public void setUpdatedItemDto(OrderItemDto updatedItemDto) {
        this.updatedItemDto = updatedItemDto;
    }
}
