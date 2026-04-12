package com.example.dummyapp.item.commandHandler;

import com.example.dummyapp.item.commands.HelperWriteItemCommand;
import com.example.dummyapp.item.service.HelperMarkChangedItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;

@Component
public class HelperMarkChangedItemCommandHandler extends CommandHandler {

    @Autowired
    private HelperMarkChangedItemService helperMarkChangedItemService;

    @Override
    protected String getAggregateTypeName() { return "Item"; }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case HelperWriteItemCommand cmd -> handleHelperWrite(cmd);
            default -> null;
        };
    }

    private Object handleHelperWrite(HelperWriteItemCommand cmd) {
        helperMarkChangedItemService.helperRegisterChanged(cmd.getItem(), cmd.getUnitOfWork());
        return null;
    }
}
