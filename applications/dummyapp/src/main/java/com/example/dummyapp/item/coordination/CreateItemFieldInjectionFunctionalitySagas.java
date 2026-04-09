package com.example.dummyapp.item.coordination;

import com.example.dummyapp.item.aggregate.ItemDto;
import com.example.dummyapp.order.commands.GetOrderCommand;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

import java.util.ArrayList;

public class CreateItemFieldInjectionFunctionalitySagas extends WorkflowFunctionality {

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    private ItemDto itemDto;
    private CommandGateway commandGateway;

    public void buildWorkflow(ItemDto itemDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.itemDto = itemDto;
        this.commandGateway = commandGateway;
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep fieldInjectedStep = new SagaStep("fieldInjectedStep", () -> {
            GetOrderCommand cmd = new GetOrderCommand(unitOfWork, "Order", itemDto.getOrderId());
            commandGateway.send(cmd);
        }, new ArrayList<>());

        workflow.addStep(fieldInjectedStep);
    }

    public ItemDto getItemDto() {
        return itemDto;
    }
}
