package com.example.dummyapp.order.coordination;

import com.example.dummyapp.item.aggregate.ItemDto;
import com.example.dummyapp.order.commands.CancelOrderCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.workflow.SagaWorkflow;

public class CancelOrderFromItemFunctionalitySagas extends WorkflowFunctionality {

    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CancelOrderFromItemFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            ItemDto itemDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(itemDto, unitOfWork);
    }

    public void buildWorkflow(ItemDto itemDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep cancelOrderStep = new SagaStep("cancelOrderStep", () -> {
            CancelOrderCommand cmd = new CancelOrderCommand(unitOfWork, "Order", itemDto.getOrderId());
            commandGateway.send(cmd);
        });

        workflow.addStep(cancelOrderStep);
    }
}
