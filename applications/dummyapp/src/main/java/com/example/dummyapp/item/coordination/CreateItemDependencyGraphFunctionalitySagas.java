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
import java.util.List;

public class CreateItemDependencyGraphFunctionalitySagas extends WorkflowFunctionality {

    private ItemDto itemDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateItemDependencyGraphFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            ItemDto itemDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(itemDto, unitOfWork);
    }

    public void buildWorkflow(ItemDto itemDto, SagaUnitOfWork unitOfWork) {
        this.itemDto = itemDto;
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep rootStep = new SagaStep("rootStep", () -> {
            GetOrderCommand cmd = new GetOrderCommand(unitOfWork, "Order", itemDto.getOrderId());
            commandGateway.send(cmd);
        }, new ArrayList<>());

        SagaStep prepareStep = new SagaStep("prepareStep", () -> {
            CreateItemCommand cmd = new CreateItemCommand(unitOfWork, "Item", itemDto);
            commandGateway.send(cmd);
        }, Arrays.asList(rootStep));

        SagaStep splitStep = new SagaStep("splitStep", () -> {
            GetOrderCommand cmd = new GetOrderCommand(unitOfWork, "Order", itemDto.getOrderId());
            commandGateway.send(cmd);
        }, List.of(rootStep));

        SagaStep mergeStep = new SagaStep("mergeStep", () -> {
            CreateItemCommand cmd = new CreateItemCommand(unitOfWork, "Item", itemDto);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(prepareStep, splitStep)));

        String unresolvedDependency = "not-a-step";
        SagaStep conservativeStep = new SagaStep("conservativeStep", () -> {
            CreateItemCommand cmd = new CreateItemCommand(unitOfWork, "Item", itemDto);
            commandGateway.send(cmd);
        }, Arrays.asList(rootStep, unresolvedDependency));

        workflow.addStep(rootStep);
        workflow.addStep(prepareStep);
        workflow.addStep(splitStep);
        workflow.addStep(mergeStep);
        workflow.addStep(conservativeStep);
    }

    public ItemDto getItemDto() {
        return itemDto;
    }
}
