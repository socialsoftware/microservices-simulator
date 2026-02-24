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

public class AddOrderProductFunctionalitySagas extends WorkflowFunctionality {
    private OrderProductDto addedProductDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddOrderProductFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer orderId, Integer productAggregateId, OrderProductDto productDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(orderId, productAggregateId, productDto, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, Integer productAggregateId, OrderProductDto productDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addProductStep = new SagaStep("addProductStep", () -> {
            AddOrderProductCommand cmd = new AddOrderProductCommand(unitOfWork, ServiceMapping.ORDER.getServiceName(), orderId, productAggregateId, productDto);
            OrderProductDto addedProductDto = (OrderProductDto) commandGateway.send(cmd);
            setAddedProductDto(addedProductDto);
        });

        workflow.addStep(addProductStep);
    }
    public OrderProductDto getAddedProductDto() {
        return addedProductDto;
    }

    public void setAddedProductDto(OrderProductDto addedProductDto) {
        this.addedProductDto = addedProductDto;
    }
}
