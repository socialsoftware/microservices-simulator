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

public class AddOrderItemFunctionalitySagas extends WorkflowFunctionality {
    private OrderItemDto addedItemDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddOrderItemFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer orderId, Integer key, OrderItemDto itemDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(orderId, key, itemDto, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, Integer key, OrderItemDto itemDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addItemStep = new SagaStep("addItemStep", () -> {
            AddOrderItemCommand cmd = new AddOrderItemCommand(unitOfWork, ServiceMapping.ORDER.getServiceName(), orderId, key, itemDto);
            OrderItemDto addedItemDto = (OrderItemDto) commandGateway.send(cmd);
            setAddedItemDto(addedItemDto);
        });

        workflow.addStep(addItemStep);
    }
    public OrderItemDto getAddedItemDto() {
        return addedItemDto;
    }

    public void setAddedItemDto(OrderItemDto addedItemDto) {
        this.addedItemDto = addedItemDto;
    }
}
