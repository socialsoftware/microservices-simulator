package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.events.subscribe.ContactsSubscribesUserDeleted;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.events.subscribe.ContactsSubscribesUserDeletedContactsUserExists;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.ContactsUserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.DocumentType;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Contacts extends Aggregate {
    private String name;
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;
    private String documentNumber;
    private String phoneNumber;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "contacts")
    private ContactsUser user;

    public Contacts() {

    }

    public Contacts(Integer aggregateId, ContactsDto contactsDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(contactsDto.getName());
        setDocumentType(DocumentType.valueOf(contactsDto.getDocumentType()));
        setDocumentNumber(contactsDto.getDocumentNumber());
        setPhoneNumber(contactsDto.getPhoneNumber());
        setUser(contactsDto.getUser() != null ? new ContactsUser(contactsDto.getUser()) : null);
    }


    public Contacts(Contacts other) {
        super(other);
        setName(other.getName());
        setDocumentType(other.getDocumentType());
        setDocumentNumber(other.getDocumentNumber());
        setPhoneNumber(other.getPhoneNumber());
        setUser(new ContactsUser(other.getUser()));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
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

    public ContactsUser getUser() {
        return user;
    }

    public void setUser(ContactsUser user) {
        this.user = user;
        if (this.user != null) {
            this.user.setContacts(this);
        }
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantContactsUserExists(eventSubscriptions);
            eventSubscriptions.add(new ContactsSubscribesUserDeleted(this));
        }
        return eventSubscriptions;
    }
    private void interInvariantContactsUserExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new ContactsSubscribesUserDeletedContactsUserExists(this.getUser()));
    }


    private boolean invariantNameNotBlank() {
        return this.name != null && this.name != null && this.name.length() > 0;
    }

    private boolean invariantDocumentNumberNotBlank() {
        return this.documentNumber != null && this.documentNumber != null && this.documentNumber.length() > 0;
    }

    private boolean invariantDocumentTypeSet() {
        return this.documentType != null;
    }

    private boolean invariantUserSet() {
        return this.user != null;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantNameNotBlank()) {
            throw new SimulatorException(INVARIANT_BREAK, "Contact name cannot be blank");
        }
        if (!invariantDocumentNumberNotBlank()) {
            throw new SimulatorException(INVARIANT_BREAK, "Document number cannot be blank");
        }
        if (!invariantDocumentTypeSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Contact must have a document type");
        }
        if (!invariantUserSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Contact must belong to a user account");
        }
    }

    public ContactsDto buildDto() {
        ContactsDto dto = new ContactsDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setName(getName());
        dto.setDocumentType(getDocumentType() != null ? getDocumentType().name() : null);
        dto.setDocumentNumber(getDocumentNumber());
        dto.setPhoneNumber(getPhoneNumber());
        dto.setUser(getUser() != null ? new ContactsUserDto(getUser()) : null);
        return dto;
    }
}