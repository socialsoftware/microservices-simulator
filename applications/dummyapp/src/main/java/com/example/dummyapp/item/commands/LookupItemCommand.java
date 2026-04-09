package com.example.dummyapp.item.commands;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class LookupItemCommand extends Command {

    private final Integer itemId;

    public LookupItemCommand(UnitOfWork unitOfWork, String serviceName, Integer itemId) {
        super(unitOfWork, serviceName, null);
        this.itemId = itemId;
    }

    public Integer getItemId() { return itemId; }
}
