package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class OrderContactsUpdatedEvent extends Event {
    private Integer contactsAggregateId;
    private Long contactsVersion;
    private String contactsName;
    private String contactsDocumentNumber;

    public OrderContactsUpdatedEvent() {
        super();
    }

    public OrderContactsUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderContactsUpdatedEvent(Integer aggregateId, Integer contactsAggregateId, Long contactsVersion, String contactsName, String contactsDocumentNumber) {
        super(aggregateId);
        setContactsAggregateId(contactsAggregateId);
        setContactsVersion(contactsVersion);
        setContactsName(contactsName);
        setContactsDocumentNumber(contactsDocumentNumber);
    }

    public Integer getContactsAggregateId() {
        return contactsAggregateId;
    }

    public void setContactsAggregateId(Integer contactsAggregateId) {
        this.contactsAggregateId = contactsAggregateId;
    }

    public Long getContactsVersion() {
        return contactsVersion;
    }

    public void setContactsVersion(Long contactsVersion) {
        this.contactsVersion = contactsVersion;
    }

    public String getContactsName() {
        return contactsName;
    }

    public void setContactsName(String contactsName) {
        this.contactsName = contactsName;
    }

    public String getContactsDocumentNumber() {
        return contactsDocumentNumber;
    }

    public void setContactsDocumentNumber(String contactsDocumentNumber) {
        this.contactsDocumentNumber = contactsDocumentNumber;
    }

}
