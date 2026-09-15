package com.example.dummyapp.inferredcopy;

import jakarta.persistence.Entity;

@Entity
public class ComputedCard {
    private Integer reference;
    private String heading;
    public ComputedCard(CardInput value) {
        setReference(value.getAggregateId());
        setHeading(value.getCaption().toUpperCase());
    }
    public void setReference(Integer value) { reference=value; }
    public void setHeading(String value) { heading=value; }
}
