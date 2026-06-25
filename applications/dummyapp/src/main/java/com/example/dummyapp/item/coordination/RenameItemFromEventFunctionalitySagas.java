package com.example.dummyapp.item.coordination;

import com.example.dummyapp.item.aggregate.ItemDto;
import com.example.dummyapp.item.commands.UpdateItemCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

public class RenameItemFromEventFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RenameItemFromEventFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            Integer itemAggregateId, String updatedName, Integer publisherAggregateId,
            Integer publisherAggregateVersion, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(itemAggregateId, updatedName, publisherAggregateId, publisherAggregateVersion, unitOfWork);
    }

    public void buildWorkflow(Integer itemAggregateId, String updatedName, Integer publisherAggregateId,
            Integer publisherAggregateVersion, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep renameItemStep = new SagaStep("renameItemFromEventStep", () -> {
            ItemDto itemDto = new ItemDto();
            itemDto.setAggregateId(itemAggregateId);
            itemDto.setName(updatedName);
            itemDto.setOrderId(publisherAggregateId);
            UpdateItemCommand cmd = new UpdateItemCommand(unitOfWork, "Item", itemAggregateId, itemDto);
            commandGateway.send(cmd);
        });

        workflow.addStep(renameItemStep);
    }
}
