package com.example.dummyapp.item.coordination;

import com.example.dummyapp.item.aggregate.ItemDto;
import com.example.dummyapp.item.commands.CreateItemCommand;
import com.example.dummyapp.item.commands.DeleteItemCommand;
import com.example.dummyapp.item.commands.GetItemCommand;
import com.example.dummyapp.item.commands.UpdateItemCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

import java.util.List;

public class CreateItemCompensationFunctionalitySagas extends WorkflowFunctionality {

    private ItemDto itemDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;
    private OverloadedItemCommandGateway overloadedCommandGateway;
    private UnrelatedItemCommandSender unrelatedCommandSender;
    private GetItemCommand outsideLambdaCommand;

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
            updateItemThroughHelper(unitOfWork);
        });

        SagaStep conservativeUnresolvedStep = new SagaStep("conservativeUnresolvedStep", this::dispatchUnresolvedWrite);

        SagaStep mixedReadHelperStep = new SagaStep("mixedReadHelperStep", () -> {
            GetItemCommand getItemCommand = new GetItemCommand(unitOfWork, "Item", itemDto.getAggregateId());
            commandGateway.send(getItemCommand);
            updateItemThroughHelper(unitOfWork);
        });

        SagaStep constructorKeyHelperStep = new SagaStep("constructorKeyHelperStep", () -> {
            GetItemCommand getItemCommand = new GetItemCommand(unitOfWork, "Item", this.getAndUpdateItemKey(unitOfWork));
            commandGateway.send(getItemCommand);
        });

        SagaStep overloadedGatewayStep = new SagaStep("overloadedGatewayStep", () -> {
            GetItemCommand getItemCommand = new GetItemCommand(unitOfWork, "Item", itemDto.getAggregateId());
            overloadedCommandGateway.send(getItemCommand);
        });

        SagaStep unrelatedSendStep = new SagaStep("unrelatedSendStep", () -> {
            GetItemCommand getItemCommand = new GetItemCommand(unitOfWork, "Item", itemDto.getAggregateId());
            unrelatedCommandSender.send(getItemCommand);
        });

        SagaStep mismatchedCommandBindingStep = new SagaStep("mismatchedCommandBindingStep", () -> {
            Runnable deferredConstruction = () -> {
                GetItemCommand outsideLambdaCommand = new GetItemCommand(unitOfWork, "Item", itemDto.getAggregateId());
            };
            commandGateway.send(outsideLambdaCommand);
        });

        SagaStep inlineReadOnlyStep = new SagaStep("inlineReadOnlyStep", () ->
                commandGateway.send(new GetItemCommand(unitOfWork, "Item", itemDto.getAggregateId())));

        SagaStep readOnlyStep = new SagaStep("readOnlyStep", () -> {
            GetItemCommand getItemCommand = new GetItemCommand(unitOfWork, "Item", itemDto.getAggregateId());
            commandGateway.send(getItemCommand);
        });

        SagaStep semanticLockReadStep = new SagaStep("semanticLockReadStep", () -> {
            GetItemCommand getItemCommand = new GetItemCommand(unitOfWork, "Item", itemDto.getAggregateId());
            SagaCommand sagaCommand = new SagaCommand(getItemCommand);
            sagaCommand.setSemanticLock(DummyItemSagaState.LOCKED);
            commandGateway.send(sagaCommand);
        });

        SagaStep semanticLockClearingStep = new SagaStep("semanticLockClearingStep", () -> {
            GetItemCommand getItemCommand = new GetItemCommand(unitOfWork, "Item", itemDto.getAggregateId());
            SagaCommand sagaCommand = new SagaCommand(getItemCommand);
            sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(sagaCommand);
        });

        SagaStep forbiddenStateReadStep = new SagaStep("forbiddenStateReadStep", () -> {
            GetItemCommand getItemCommand = new GetItemCommand(unitOfWork, "Item", itemDto.getAggregateId());
            SagaCommand sagaCommand = new SagaCommand(getItemCommand);
            sagaCommand.setForbiddenStates(List.of(DummyItemSagaState.LOCKED));
            commandGateway.send(sagaCommand);
        });

        SagaStep postDispatchLockStep = new SagaStep("postDispatchLockStep", () -> {
            GetItemCommand getItemCommand = new GetItemCommand(unitOfWork, "Item", itemDto.getAggregateId());
            SagaCommand sagaCommand = new SagaCommand(getItemCommand);
            commandGateway.send(sagaCommand);
            sagaCommand.setSemanticLock(DummyItemSagaState.LOCKED);
        });

        SagaStep uncertainWrapperConfigurationStep = new SagaStep("uncertainWrapperConfigurationStep", () -> {
            GetItemCommand getItemCommand = new GetItemCommand(unitOfWork, "Item", itemDto.getAggregateId());
            SagaCommand sagaCommand = new SagaCommand(getItemCommand);
            configureSemanticLock(sagaCommand);
            commandGateway.send(sagaCommand);
        });

        workflow.addStep(createItemStep);
        workflow.addStep(explicitWithoutRecognizedDispatchStep);
        workflow.addStep(implicitWriteStep);
        workflow.addStep(conservativeUnresolvedStep);
        workflow.addStep(mixedReadHelperStep);
        workflow.addStep(constructorKeyHelperStep);
        workflow.addStep(overloadedGatewayStep);
        workflow.addStep(unrelatedSendStep);
        workflow.addStep(mismatchedCommandBindingStep);
        workflow.addStep(inlineReadOnlyStep);
        workflow.addStep(readOnlyStep);
        workflow.addStep(semanticLockReadStep);
        workflow.addStep(semanticLockClearingStep);
        workflow.addStep(forbiddenStateReadStep);
        workflow.addStep(postDispatchLockStep);
        workflow.addStep(uncertainWrapperConfigurationStep);
    }

    private Integer getAndUpdateItemKey(SagaUnitOfWork unitOfWork) {
        updateItemThroughHelper(unitOfWork);
        return itemDto.getAggregateId();
    }

    private void updateItemThroughHelper(SagaUnitOfWork unitOfWork) {
        UpdateItemCommand updateItemCommand = new UpdateItemCommand(unitOfWork, "Item", itemDto);
        commandGateway.send(updateItemCommand);
    }

    private void dispatchUnresolvedWrite() {
        // Method-reference bodies are intentionally outside the visitor's local analysis boundary.
    }

    private void configureSemanticLock(SagaCommand sagaCommand) {
        sagaCommand.setSemanticLock(DummyItemSagaState.LOCKED);
    }

    public ItemDto getItemDto() {
        return itemDto;
    }
}

enum DummyItemSagaState implements SagaAggregate.SagaState {
    LOCKED
}

class OverloadedItemCommandGateway extends CommandGateway {

    OverloadedItemCommandGateway() {
        super(null);
    }

    @Override
    public Object send(Command command) {
        return null;
    }

    public Object send(GetItemCommand command) {
        return null;
    }
}

class UnrelatedItemCommandSender {

    public Object send(GetItemCommand command) {
        return null;
    }
}
