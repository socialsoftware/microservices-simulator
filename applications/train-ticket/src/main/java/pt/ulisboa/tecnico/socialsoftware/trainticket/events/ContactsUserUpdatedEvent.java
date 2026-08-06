package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class ContactsUserUpdatedEvent extends Event {
    private Integer userAggregateId;
    private Long userVersion;

    public ContactsUserUpdatedEvent() {
        super();
    }

    public ContactsUserUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ContactsUserUpdatedEvent(Integer aggregateId, Integer userAggregateId, Long userVersion) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
        setUserVersion(userVersion);
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Long getUserVersion() {
        return userVersion;
    }

    public void setUserVersion(Long userVersion) {
        this.userVersion = userVersion;
    }

}
