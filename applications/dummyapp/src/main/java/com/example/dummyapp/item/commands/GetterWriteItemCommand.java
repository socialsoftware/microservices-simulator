package com.example.dummyapp.item.commands;

import com.example.dummyapp.item.aggregate.Item;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

public class GetterWriteItemCommand extends Command {

    private final Item item;

    public GetterWriteItemCommand(UnitOfWork unitOfWork, Item item) {
        super(unitOfWork, "Item", null);
        this.item = item;
    }

    public Item getItem() { return item; }
}
