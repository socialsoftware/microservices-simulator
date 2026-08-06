package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class OrderContactsDeletedEvent extends Event {
    private Integer contactsAggregateId;

    public OrderContactsDeletedEvent() {
        super();
    }

    public OrderContactsDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderContactsDeletedEvent(Integer aggregateId, Integer contactsAggregateId) {
        super(aggregateId);
        setContactsAggregateId(contactsAggregateId);
    }

    public Integer getContactsAggregateId() {
        return contactsAggregateId;
    }

    public void setContactsAggregateId(Integer contactsAggregateId) {
        this.contactsAggregateId = contactsAggregateId;
    }

}
