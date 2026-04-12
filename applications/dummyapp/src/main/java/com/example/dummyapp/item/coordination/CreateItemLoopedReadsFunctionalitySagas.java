package com.example.dummyapp.item.coordination;

import com.example.dummyapp.item.aggregate.ItemDto;
import com.example.dummyapp.order.commands.GetOrderCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.workflow.SagaWorkflow;

import java.util.Arrays;
import java.util.List;

public class CreateItemLoopedReadsFunctionalitySagas extends WorkflowFunctionality {

    private ItemDto itemDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateItemLoopedReadsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            ItemDto itemDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(itemDto, unitOfWork);
    }

    public void buildWorkflow(ItemDto itemDto, SagaUnitOfWork unitOfWork) {
        this.itemDto = itemDto;
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep loopedStaticReadStep = new SagaStep("loopedStaticReadStep", () -> {
            for (int i = 0; i < 3; i++) {
                GetOrderCommand cmd = new GetOrderCommand(unitOfWork, "Order", itemDto.getOrderId());
                commandGateway.send(cmd);
            }
        });

        SagaStep loopedRuntimeReadStep = new SagaStep("loopedRuntimeReadStep", () -> {
            List<Integer> orderIds = Arrays.asList(itemDto.getOrderId(), itemDto.getOrderId() + 1);
            orderIds.forEach(orderId -> {
                GetOrderCommand cmd = new GetOrderCommand(unitOfWork, "Order", orderId);
                commandGateway.send(cmd);
            });
        });

        workflow.addStep(loopedStaticReadStep);
        workflow.addStep(loopedRuntimeReadStep);
    }

    public ItemDto getItemDto() {
        return itemDto;
    }
}
