package com.example.dummyapp.item.aggregate;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Item extends Aggregate {

    private String name;
    private int price;

    public Item() {}

    public Item(Integer aggregateId, String name, int price) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.name = name;
        this.price = price;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    @Override
    public void verifyInvariants() {}

    @Override
    public Set<EventSubscription> getEventSubscriptions() { return new HashSet<>(); }
}
