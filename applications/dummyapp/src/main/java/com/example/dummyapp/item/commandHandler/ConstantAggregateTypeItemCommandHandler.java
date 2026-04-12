package com.example.dummyapp.item.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import com.example.dummyapp.item.commands.DeleteItemCommand;
import com.example.dummyapp.item.service.ItemService;

@Component
public class ConstantAggregateTypeItemCommandHandler extends CommandHandler {

    private static final String AGGREGATE_TYPE = "Item";

    @Autowired
    private ItemService itemService;

    @Override
    protected String getAggregateTypeName() {
        return AGGREGATE_TYPE;
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case DeleteItemCommand cmd -> handleDeleteItem(cmd);
            default -> null;
        };
    }

    private Object handleDeleteItem(DeleteItemCommand cmd) {
        itemService.deleteItem(cmd.getItemAggregateId(), cmd.getUnitOfWork());
        return null;
    }
}
