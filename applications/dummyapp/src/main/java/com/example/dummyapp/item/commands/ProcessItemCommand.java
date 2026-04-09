package com.example.dummyapp.item.commands;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import com.example.dummyapp.item.aggregate.ItemDto;

public class ProcessItemCommand extends Command {

    private final ItemDto itemDto;

    public ProcessItemCommand(UnitOfWork unitOfWork, String serviceName, ItemDto itemDto) {
        super(unitOfWork, serviceName, null);
        this.itemDto = itemDto;
    }

    public ItemDto getItemDto() { return itemDto; }
}
