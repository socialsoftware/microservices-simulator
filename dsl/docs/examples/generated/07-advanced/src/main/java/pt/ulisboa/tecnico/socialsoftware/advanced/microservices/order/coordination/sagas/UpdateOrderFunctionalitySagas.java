package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.order.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateOrderFunctionalitySagas extends WorkflowFunctionality {
    private OrderDto updatedOrderDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateOrderFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, OrderDto orderDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(orderDto, unitOfWork);
    }

    public void buildWorkflow(OrderDto orderDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateOrderStep = new SagaStep("updateOrderStep", () -> {
            UpdateOrderCommand cmd = new UpdateOrderCommand(unitOfWork, ServiceMapping.ORDER.getServiceName(), orderDto);
            OrderDto updatedOrderDto = (OrderDto) commandGateway.send(cmd);
            setUpdatedOrderDto(updatedOrderDto);
        });

        workflow.addStep(updateOrderStep);
    }
    public OrderDto getUpdatedOrderDto() {
        return updatedOrderDto;
    }

    public void setUpdatedOrderDto(OrderDto updatedOrderDto) {
        this.updatedOrderDto = updatedOrderDto;
    }
}
