package com.example.dummyapp.item.notification.handling.handlers;

import com.example.dummyapp.item.aggregate.ItemRepository;
import com.example.dummyapp.item.coordination.eventProcessing.ItemEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;

public abstract class ItemEventHandler extends EventHandler {
    protected ItemEventProcessing itemEventProcessing;

    public ItemEventHandler(ItemRepository itemRepository, ItemEventProcessing itemEventProcessing) {
        super(itemRepository);
        this.itemEventProcessing = itemEventProcessing;
    }
}
