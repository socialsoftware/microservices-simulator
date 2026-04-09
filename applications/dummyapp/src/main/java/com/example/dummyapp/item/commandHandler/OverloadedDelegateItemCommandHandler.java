package com.example.dummyapp.item.commandHandler;

import com.example.dummyapp.item.aggregate.ItemDto;
import com.example.dummyapp.item.commands.CreateItemCommand;
import com.example.dummyapp.item.commands.LookupItemCommand;
import com.example.dummyapp.item.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;

@Component
public class OverloadedDelegateItemCommandHandler extends CommandHandler {

    @Autowired
    private ItemService itemService;

    @Override
    protected String getAggregateTypeName() {
        return "Item";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateItemCommand cmd -> handleCreate(cmd);
            default -> null;
        };
    }

    private Object handleCreate(LookupItemCommand cmd) {
        return null;
    }

    private Object handleCreate(CreateItemCommand cmd) {
        ItemDto dto = cmd.getItemDto();
        UnitOfWork unitOfWork = cmd.getUnitOfWork();
        return itemService.createItem(dto, unitOfWork);
    }
}
