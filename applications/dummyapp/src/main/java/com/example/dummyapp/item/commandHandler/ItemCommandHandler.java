package com.example.dummyapp.item.commandHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import com.example.dummyapp.item.commands.CreateItemCommand;
import com.example.dummyapp.item.commands.DeleteItemCommand;
import com.example.dummyapp.item.commands.GetItemCommand;
import com.example.dummyapp.item.commands.UpdateItemCommand;
import com.example.dummyapp.item.service.ItemService;

@Component
public class ItemCommandHandler extends CommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(ItemCommandHandler.class);

    @Autowired
    private ItemService itemService;

    @Override
    protected String getAggregateTypeName() {
        return "Item";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetItemCommand cmd -> handleGetItem(cmd);
            case CreateItemCommand cmd -> handleCreateItem(cmd);
            case UpdateItemCommand cmd -> handleUpdateItem(cmd);
            case DeleteItemCommand cmd -> handleDeleteItem(cmd);
            default -> {
                logger.warn("Unknown command type: {}", command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetItem(GetItemCommand cmd) {
        return itemService.getItem(cmd.getItemAggregateId(), cmd.getUnitOfWork());
    }

    private Object handleCreateItem(CreateItemCommand cmd) {
        return itemService.createItem(cmd.getItemDto(), cmd.getUnitOfWork());
    }

    private Object handleUpdateItem(UpdateItemCommand cmd) {
        return itemService.updateItem(cmd.getItemAggregateId(), cmd.getItemDto(), cmd.getUnitOfWork());
    }

    private Object handleDeleteItem(DeleteItemCommand cmd) {
        itemService.deleteItem(cmd.getItemAggregateId(), cmd.getUnitOfWork());
        return null;
    }
}
