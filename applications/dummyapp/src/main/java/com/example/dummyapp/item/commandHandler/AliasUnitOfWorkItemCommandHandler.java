package com.example.dummyapp.item.commandHandler;

import com.example.dummyapp.item.commands.AliasWriteItemCommand;
import com.example.dummyapp.item.service.AliasUnitOfWorkItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;

@Component
public class AliasUnitOfWorkItemCommandHandler extends CommandHandler {

    @Autowired
    private AliasUnitOfWorkItemService aliasUnitOfWorkItemService;

    @Override
    protected String getAggregateTypeName() { return "Item"; }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case AliasWriteItemCommand cmd -> handleAliasWrite(cmd);
            default -> null;
        };
    }

    private Object handleAliasWrite(AliasWriteItemCommand cmd) {
        aliasUnitOfWorkItemService.aliasRegisterChanged(cmd.getItem(), cmd.getUnitOfWork());
        return null;
    }
}
