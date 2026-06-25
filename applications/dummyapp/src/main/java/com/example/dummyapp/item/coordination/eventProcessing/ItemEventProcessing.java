package com.example.dummyapp.item.coordination.eventProcessing;

import com.example.dummyapp.events.ItemRenamedEvent;
import com.example.dummyapp.item.coordination.ItemFunctionalitiesFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ItemEventProcessing {
    @Autowired
    private ItemFunctionalitiesFacade itemFunctionalities;

    public void processItemRenamedEvent(Integer subscriberAggregateId, ItemRenamedEvent event) {
        itemFunctionalities.renameItemFromEvent(subscriberAggregateId, event);
    }
}
