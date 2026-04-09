package com.example.dummyapp.item.commands;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetItemCommand extends Command {

    private final Integer itemAggregateId;

    public GetItemCommand(UnitOfWork unitOfWork, String serviceName, Integer itemAggregateId) {
        super(unitOfWork, serviceName, itemAggregateId);
        this.itemAggregateId = itemAggregateId;
    }

    public Integer getItemAggregateId() { return itemAggregateId; }
}
