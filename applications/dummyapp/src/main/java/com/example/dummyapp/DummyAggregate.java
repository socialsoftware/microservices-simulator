package com.example.dummyapp;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;

import java.util.HashSet;
import java.util.Set;

@Entity
public class DummyAggregate extends Aggregate {

    private String label;

    public DummyAggregate() {}

    public DummyAggregate(Integer aggregateId, String label) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.label = label;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    @Override
    public void verifyInvariants() {}

    @Override
    public Set<EventSubscription> getEventSubscriptions() { return new HashSet<>(); }
}
