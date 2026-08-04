package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.Contacts;

public class ContactsDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String name;
    private String documentType;
    private String documentNumber;
    private String phoneNumber;
    private ContactsUserDto user;

    public ContactsDto() {
    }

    public ContactsDto(Contacts contacts) {
        this.aggregateId = contacts.getAggregateId();
        this.version = contacts.getVersion();
        this.state = contacts.getState();
        this.name = contacts.getName();
        this.documentType = contacts.getDocumentType() != null ? contacts.getDocumentType().name() : null;
        this.documentNumber = contacts.getDocumentNumber();
        this.phoneNumber = contacts.getPhoneNumber();
        this.user = contacts.getUser() != null ? new ContactsUserDto(contacts.getUser()) : null;
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

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public ContactsUserDto getUser() {
        return user;
    }

    public void setUser(ContactsUserDto user) {
        this.user = user;
    }
}