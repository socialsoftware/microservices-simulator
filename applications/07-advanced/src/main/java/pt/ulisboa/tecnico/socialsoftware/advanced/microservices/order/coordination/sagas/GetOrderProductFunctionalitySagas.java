package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.order.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderProductDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetOrderProductFunctionalitySagas extends WorkflowFunctionality {
    private OrderProductDto productDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetOrderProductFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer orderId, Integer productAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(orderId, productAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, Integer productAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getProductStep = new SagaStep("getProductStep", () -> {
            GetOrderProductCommand cmd = new GetOrderProductCommand(unitOfWork, ServiceMapping.ORDER.getServiceName(), orderId, productAggregateId);
            OrderProductDto productDto = (OrderProductDto) commandGateway.send(cmd);
            setProductDto(productDto);
        });

        workflow.addStep(getProductStep);
    }
    public OrderProductDto getProductDto() {
        return productDto;
    }

    public void setProductDto(OrderProductDto productDto) {
        this.productDto = productDto;
    }
}
