package com.example.dummyapp.item.commands;

import com.example.dummyapp.item.aggregate.Item;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class AliasWriteItemCommand extends Command {

    private final Item item;

    public AliasWriteItemCommand(UnitOfWork unitOfWork, Item item) {
        super(unitOfWork, "Item", null);
        this.item = item;
    }

    public Item getItem() { return item; }
}
