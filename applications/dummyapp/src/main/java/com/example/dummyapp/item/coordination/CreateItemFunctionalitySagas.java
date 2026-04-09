package com.example.dummyapp.item.coordination;

import com.example.dummyapp.item.aggregate.ItemDto;
import com.example.dummyapp.item.commands.CreateItemCommand;
import com.example.dummyapp.order.commands.GetOrderCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

import java.util.ArrayList;
import java.util.Arrays;

public class CreateItemFunctionalitySagas extends WorkflowFunctionality {

    private ItemDto itemDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateItemFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            ItemDto itemDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(itemDto, unitOfWork);
    }

    public void buildWorkflow(ItemDto itemDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // Step 1: Verify the referenced order exists (READ — no write, no compensation needed)
        SagaStep getOrderStep = new SagaStep("getOrderStep", () -> {
            GetOrderCommand cmd = new GetOrderCommand(unitOfWork, "Order", itemDto.getOrderId());
            commandGateway.send(cmd);
        });

        // Step 2: Create the item (WRITE — depends on step 1)
        SagaStep createItemStep = new SagaStep("createItemStep", () -> {
            CreateItemCommand cmd = new CreateItemCommand(unitOfWork, "Item", itemDto);
            this.itemDto = (ItemDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getOrderStep)));

        workflow.addStep(getOrderStep);
        workflow.addStep(createItemStep);
    }

    public ItemDto getItemDto() { return itemDto; }
}
