package com.example.dummyapp.item.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import com.example.dummyapp.item.commands.LookupItemCommand;
import com.example.dummyapp.item.commands.ProcessItemCommand;
import com.example.dummyapp.item.service.OverloadedItemService;

@Component
public class OverloadedItemCommandHandler extends CommandHandler {

    @Autowired
    private OverloadedItemService overloadedItemService;

    @Override
    protected String getAggregateTypeName() {
        return "Item";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case ProcessItemCommand cmd -> handleProcess(cmd);
            case LookupItemCommand cmd -> handleLookup(cmd);
            default -> null;
        };
    }

    private Object handleProcess(ProcessItemCommand cmd) {
        return overloadedItemService.processItem(cmd.getItemDto(), cmd.getUnitOfWork());
    }

    private Object handleLookup(LookupItemCommand cmd) {
        return overloadedItemService.processItem(cmd.getItemId(), cmd.getUnitOfWork());
    }
}
