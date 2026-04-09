package com.example.dummyapp.order.aggregate;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Order extends Aggregate {

    private String status;

    public Order() {}

    public Order(Integer aggregateId, String status) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.status = status;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public void verifyInvariants() {}

    @Override
    public Set<EventSubscription> getEventSubscriptions() { return new HashSet<>(); }
}
