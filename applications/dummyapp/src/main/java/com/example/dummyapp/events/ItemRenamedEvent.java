package com.example.dummyapp.events;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

public class ItemRenamedEvent extends Event {
    private Integer itemAggregateId;
    private String updatedName;

    public ItemRenamedEvent() {
    }

    public ItemRenamedEvent(Integer publisherAggregateId, Integer itemAggregateId, String updatedName) {
        super(publisherAggregateId);
        this.itemAggregateId = itemAggregateId;
        this.updatedName = updatedName;
    }

    public Integer getItemAggregateId() {
        return itemAggregateId;
    }

    public String getUpdatedName() {
        return updatedName;
    }
}
