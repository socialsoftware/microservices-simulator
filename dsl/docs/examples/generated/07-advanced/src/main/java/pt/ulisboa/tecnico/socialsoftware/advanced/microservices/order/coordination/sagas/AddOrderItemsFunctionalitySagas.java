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
import java.util.List;

public class AddOrderItemsFunctionalitySagas extends WorkflowFunctionality {
    private List<OrderItemDto> addedItemDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddOrderItemsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer orderId, List<OrderItemDto> itemDtos, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(orderId, itemDtos, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, List<OrderItemDto> itemDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addItemsStep = new SagaStep("addItemsStep", () -> {
            AddOrderItemsCommand cmd = new AddOrderItemsCommand(unitOfWork, ServiceMapping.ORDER.getServiceName(), orderId, itemDtos);
            List<OrderItemDto> addedItemDtos = (List<OrderItemDto>) commandGateway.send(cmd);
            setAddedItemDtos(addedItemDtos);
        });

        workflow.addStep(addItemsStep);
    }
    public List<OrderItemDto> getAddedItemDtos() {
        return addedItemDtos;
    }

    public void setAddedItemDtos(List<OrderItemDto> addedItemDtos) {
        this.addedItemDtos = addedItemDtos;
    }
}
