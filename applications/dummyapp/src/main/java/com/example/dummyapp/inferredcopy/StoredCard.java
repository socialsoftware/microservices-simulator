package com.example.dummyapp.inferredcopy;

import jakarta.persistence.Entity;

/** Source-only verifier fixture. Different names intentionally exercise inferred copies. */
@Entity
public class StoredCard {
    private Integer reference;
    private String heading;
    public StoredCard(CardInput value) {
        setReference(value.getAggregateId());
        setHeading(value.getCaption());
    }
    public void setReference(Integer value) { reference=value; }
    public void setHeading(String value) { heading=value; }
}
