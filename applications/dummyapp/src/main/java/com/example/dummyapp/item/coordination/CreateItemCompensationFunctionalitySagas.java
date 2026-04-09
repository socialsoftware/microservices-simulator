package com.example.dummyapp.item.coordination;

import com.example.dummyapp.item.aggregate.ItemDto;
import com.example.dummyapp.item.commands.CreateItemCommand;
import com.example.dummyapp.item.commands.DeleteItemCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

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

        workflow.addStep(createItemStep);
    }

    public ItemDto getItemDto() {
        return itemDto;
    }
}
