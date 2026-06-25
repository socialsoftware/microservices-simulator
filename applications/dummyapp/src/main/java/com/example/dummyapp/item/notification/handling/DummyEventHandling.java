package com.example.dummyapp.item.notification.handling;

import com.example.dummyapp.events.ItemRenamedEvent;
import com.example.dummyapp.item.aggregate.ItemRepository;
import com.example.dummyapp.item.coordination.eventProcessing.ItemEventProcessing;
import com.example.dummyapp.item.notification.handling.handlers.ItemRenamedEventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;

@Component
public class DummyEventHandling implements EventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemEventProcessing itemEventProcessing;

    @Scheduled(fixedDelay = 1000)
    public void handleItemRenamedEvents() {
        eventApplicationService.handleSubscribedEvent(ItemRenamedEvent.class,
                new ItemRenamedEventHandler(itemRepository, itemEventProcessing));
    }
}
