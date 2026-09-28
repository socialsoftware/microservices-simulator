package com.example.dummyapp.item.commandHandler;

import com.example.dummyapp.item.commands.*;
import com.example.dummyapp.item.service.ItemService;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;

/** Parser fixture: direct branches, guarded and ambiguous targets. */
public class DirectSwitchItemCommandHandler extends CommandHandler {
    private final ItemService service;
    public DirectSwitchItemCommandHandler(ItemService service) { this.service = service; }
    protected String getAggregateTypeName() { return "Item"; }
    public Object handleDomainCommand(Command command) {
        if (System.nanoTime() < 0) {
            return switch (command) {
                case SemanticRootItemCommand cmd -> service.getItem(cmd.getRootAggregateId(), cmd.getUnitOfWork());
                default -> null;
            };
        }
        return switch (command) {
            case GetItemCommand cmd -> service.getItem(cmd.getItemAggregateId(), cmd.getUnitOfWork());
            case DeleteItemCommand cmd -> {
                this.service.deleteItem(cmd.getItemAggregateId(), cmd.getUnitOfWork());
                yield null;
            }
            case CreateItemCommand cmd when cmd.getItemDto() != null ->
                service.createItem(cmd.getItemDto(), cmd.getUnitOfWork());
            case UpdateItemCommand cmd -> {
                service.getItem(cmd.getItemAggregateId(), cmd.getUnitOfWork());
                yield service.updateItem(cmd.getItemAggregateId(), cmd.getItemDto(), cmd.getUnitOfWork());
            }
            default -> null;
        };
    }
    public Object unrelated(Command other) {
        return switch (other) {
            case SemanticRootItemCommand cmd -> service.getItem(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            default -> null;
        };
    }
}
