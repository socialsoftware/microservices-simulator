package com.example.dummyapp.item.notification.handling.handlers;

import com.example.dummyapp.events.ItemRenamedEvent;
import com.example.dummyapp.item.aggregate.ItemRepository;
import com.example.dummyapp.item.coordination.eventProcessing.ItemEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

public class ItemRenamedEventHandler extends ItemEventHandler {
    public ItemRenamedEventHandler(ItemRepository itemRepository, ItemEventProcessing itemEventProcessing) {
        super(itemRepository, itemEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.itemEventProcessing.processItemRenamedEvent(subscriberAggregateId, (ItemRenamedEvent) event);
    }
}
