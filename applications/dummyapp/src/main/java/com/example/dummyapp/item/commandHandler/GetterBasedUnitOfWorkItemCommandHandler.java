package com.example.dummyapp.item.commandHandler;

import com.example.dummyapp.item.commands.GetterWriteItemCommand;
import com.example.dummyapp.item.service.GetterBasedUnitOfWorkItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;

@Component
public class GetterBasedUnitOfWorkItemCommandHandler extends CommandHandler {

    @Autowired
    private GetterBasedUnitOfWorkItemService getterBasedUnitOfWorkItemService;

    @Override
    protected String getAggregateTypeName() { return "Item"; }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetterWriteItemCommand cmd -> handleGetterWrite(cmd);
            default -> null;
        };
    }

    private Object handleGetterWrite(GetterWriteItemCommand cmd) {
        getterBasedUnitOfWorkItemService.getterRegisterChanged(cmd.getItem(), cmd.getUnitOfWork());
        return null;
    }
}
