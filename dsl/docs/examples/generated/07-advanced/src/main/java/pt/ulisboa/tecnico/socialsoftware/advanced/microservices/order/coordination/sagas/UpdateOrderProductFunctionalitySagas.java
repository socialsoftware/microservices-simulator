package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.order.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderProductDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateOrderProductFunctionalitySagas extends WorkflowFunctionality {
    private OrderProductDto updatedProductDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateOrderProductFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer orderId, Integer productAggregateId, OrderProductDto productDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(orderId, productAggregateId, productDto, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, Integer productAggregateId, OrderProductDto productDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateProductStep = new SagaStep("updateProductStep", () -> {
            UpdateOrderProductCommand cmd = new UpdateOrderProductCommand(unitOfWork, ServiceMapping.ORDER.getServiceName(), orderId, productAggregateId, productDto);
            OrderProductDto updatedProductDto = (OrderProductDto) commandGateway.send(cmd);
            setUpdatedProductDto(updatedProductDto);
        });

        workflow.addStep(updateProductStep);
    }
    public OrderProductDto getUpdatedProductDto() {
        return updatedProductDto;
    }

    public void setUpdatedProductDto(OrderProductDto updatedProductDto) {
        this.updatedProductDto = updatedProductDto;
    }
}
