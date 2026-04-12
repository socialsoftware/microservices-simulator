package com.example.dummyapp.item.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
import com.example.dummyapp.item.aggregate.ItemDto;
import com.example.dummyapp.item.commands.CreateItemCommand;
import com.example.dummyapp.item.service.ItemService;

@Component
public class DelegatingItemCommandHandler extends CommandHandler {

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

    private Object handleCreate(CreateItemCommand cmd) {
        return doCreate(cmd.getItemDto(), cmd.getUnitOfWork());
    }

    private Object doCreate(ItemDto dto, UnitOfWork uow) {
        return itemService.createItem(dto, uow);
    }
}
