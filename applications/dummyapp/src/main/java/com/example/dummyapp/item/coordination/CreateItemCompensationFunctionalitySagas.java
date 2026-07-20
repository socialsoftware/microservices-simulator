package com.example.dummyapp.item.coordination;

import com.example.dummyapp.item.aggregate.ItemDto;
import com.example.dummyapp.item.commands.CreateItemCommand;
import com.example.dummyapp.item.commands.DeleteItemCommand;
import com.example.dummyapp.item.commands.GetItemCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

public class CreateItemCompensationFunctionalitySagas extends WorkflowFunctionality {

    private ItemDto itemDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateItemCompensationFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            ItemDto itemDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(itemDto, unitOfWork);
    }

    public void buildWorkflow(ItemDto itemDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createItemStep = new SagaStep("createItemStep", () -> {
            CreateItemCommand createItemCommand = new CreateItemCommand(unitOfWork, "Item", itemDto);
            this.itemDto = (ItemDto) commandGateway.send(createItemCommand);
        });

        createItemStep.registerCompensation(() -> {
            DeleteItemCommand deleteItemCommand = new DeleteItemCommand(unitOfWork, "Item", this.itemDto.getAggregateId());
            commandGateway.send(deleteItemCommand);
        }, unitOfWork);

        SagaStep explicitWithoutRecognizedDispatchStep = new SagaStep("explicitWithoutRecognizedDispatchStep", () -> {
            GetItemCommand getItemCommand = new GetItemCommand(unitOfWork, "Item", itemDto.getAggregateId());
            commandGateway.send(getItemCommand);
        });
        explicitWithoutRecognizedDispatchStep.registerCompensation(() -> {
            this.itemDto = this.itemDto;
        }, unitOfWork);

        SagaStep implicitWriteStep = new SagaStep("implicitWriteStep", () -> {
            CreateItemCommand createItemCommand = new CreateItemCommand(unitOfWork, "Item", itemDto);
            commandGateway.send(createItemCommand);
        });

        SagaStep conservativeUnresolvedStep = new SagaStep("conservativeUnresolvedStep", this::dispatchUnresolvedWrite);

        SagaStep readOnlyStep = new SagaStep("readOnlyStep", () -> {
            GetItemCommand getItemCommand = new GetItemCommand(unitOfWork, "Item", itemDto.getAggregateId());
            commandGateway.send(getItemCommand);
        });

        workflow.addStep(createItemStep);
        workflow.addStep(explicitWithoutRecognizedDispatchStep);
        workflow.addStep(implicitWriteStep);
        workflow.addStep(conservativeUnresolvedStep);
        workflow.addStep(readOnlyStep);
    }

    private void dispatchUnresolvedWrite() {
        // Method-reference bodies are intentionally outside the visitor's local analysis boundary.
    }

    public ItemDto getItemDto() {
        return itemDto;
    }
}
