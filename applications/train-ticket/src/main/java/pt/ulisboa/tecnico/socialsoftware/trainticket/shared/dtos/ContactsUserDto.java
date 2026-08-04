package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsUser;

public class ContactsUserDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private String state;

    public ContactsUserDto() {
    }

    public ContactsUserDto(ContactsUser contactsUser) {
        this.aggregateId = contactsUser.getUserAggregateId();
        this.version = contactsUser.getUserVersion();
        this.state = contactsUser.getUserState() != null ? contactsUser.getUserState().name() : null;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}