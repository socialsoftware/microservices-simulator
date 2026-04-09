package com.example.dummyapp.item.commands;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import com.example.dummyapp.item.aggregate.ItemDto;

public class UpdateItemCommand extends Command {

    private final Integer itemAggregateId;
    private final ItemDto itemDto;

    public UpdateItemCommand(UnitOfWork unitOfWork, String serviceName, Integer itemAggregateId, ItemDto itemDto) {
        super(unitOfWork, serviceName, itemAggregateId);
        this.itemAggregateId = itemAggregateId;
        this.itemDto = itemDto;
    }

    public Integer getItemAggregateId() { return itemAggregateId; }
    public ItemDto getItemDto() { return itemDto; }
}
