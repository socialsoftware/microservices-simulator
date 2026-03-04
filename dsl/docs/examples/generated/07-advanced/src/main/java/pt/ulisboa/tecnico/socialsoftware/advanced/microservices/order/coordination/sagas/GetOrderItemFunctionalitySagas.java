package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.order.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderItemDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetOrderItemFunctionalitySagas extends WorkflowFunctionality {
    private OrderItemDto itemDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetOrderItemFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer orderId, Integer key, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(orderId, key, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, Integer key, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getItemStep = new SagaStep("getItemStep", () -> {
            GetOrderItemCommand cmd = new GetOrderItemCommand(unitOfWork, ServiceMapping.ORDER.getServiceName(), orderId, key);
            OrderItemDto itemDto = (OrderItemDto) commandGateway.send(cmd);
            setItemDto(itemDto);
        });

        workflow.addStep(getItemStep);
    }
    public OrderItemDto getItemDto() {
        return itemDto;
    }

    public void setItemDto(OrderItemDto itemDto) {
        this.itemDto = itemDto;
    }
}
