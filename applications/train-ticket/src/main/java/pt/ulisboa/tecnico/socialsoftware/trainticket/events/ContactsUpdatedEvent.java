package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class ContactsUpdatedEvent extends Event {
    private String name;
    private String documentNumber;
    private String phoneNumber;

    public ContactsUpdatedEvent() {
        super();
    }

    public ContactsUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ContactsUpdatedEvent(Integer aggregateId, String name, String documentNumber, String phoneNumber) {
        super(aggregateId);
        setName(name);
        setDocumentNumber(documentNumber);
        setPhoneNumber(phoneNumber);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

}
