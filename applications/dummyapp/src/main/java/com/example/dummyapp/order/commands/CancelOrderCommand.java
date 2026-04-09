package com.example.dummyapp.order.commands;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class CancelOrderCommand extends Command {

    private final Integer orderAggregateId;

    public CancelOrderCommand(UnitOfWork unitOfWork, String serviceName, Integer orderAggregateId) {
        super(unitOfWork, serviceName, orderAggregateId);
        this.orderAggregateId = orderAggregateId;
    }

    public Integer getOrderAggregateId() { return orderAggregateId; }
}
