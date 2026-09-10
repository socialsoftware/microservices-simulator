package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.enums.DocumentType;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException;

import java.util.HashSet;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.CONTACTS_DOCUMENT_NUMBER_PRESENT;

@Entity
@Table(name = "contacts")
public abstract class Contacts extends Aggregate {
    // The owning account reference is immutable (domain model §2), so it is final and has no setter.
    @Column(name = "user_aggregate_id")
    private final Integer userAggregateId;
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private DocumentType documentType;
    @Column(name = "document_number")
    private String documentNumber;
    @Column(name = "phone_number")
    private String phoneNumber;

    public Contacts() {
        this.userAggregateId = null;
    }

    public Contacts(Integer aggregateId, ContactsDto contactsDto) {
        super(aggregateId);
        this.userAggregateId = contactsDto.getUserAggregateId();
        setName(contactsDto.getName());
        setDocumentType(contactsDto.getDocumentType());
        setDocumentNumber(contactsDto.getDocumentNumber());
        setPhoneNumber(contactsDto.getPhoneNumber());
        setAggregateType(getClass().getSimpleName());
    }

    public Contacts(Contacts other) {
        super(other);
        this.userAggregateId = other.getUserAggregateId();
        setName(other.getName());
        setDocumentType(other.getDocumentType());
        setDocumentNumber(other.getDocumentNumber());
        setPhoneNumber(other.getPhoneNumber());
    }

    @Override
    public void verifyInvariants() {
        if (this.documentType != DocumentType.NONE
                && (this.documentNumber == null || this.documentNumber.isBlank())) {
            throw new TrainticketException(CONTACTS_DOCUMENT_NUMBER_PRESENT);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    public Integer getUserAggregateId() {
        return this.userAggregateId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DocumentType getDocumentType() {
        return this.documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return this.documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
