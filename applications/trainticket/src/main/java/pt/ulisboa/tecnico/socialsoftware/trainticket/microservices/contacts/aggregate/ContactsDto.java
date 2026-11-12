package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.trainticket.enums.DocumentType;

public class ContactsDto {
    private Integer aggregateId;
    private Long version;
    private Aggregate.AggregateState state;
    private Integer userAggregateId;
    private String name;
    private DocumentType documentType;
    private String documentNumber;
    private String phoneNumber;

    public ContactsDto() {
    }

    public ContactsDto(Integer userAggregateId, String name, DocumentType documentType,
                       String documentNumber, String phoneNumber) {
        this.userAggregateId = userAggregateId;
        this.name = name;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.phoneNumber = phoneNumber;
    }

    public ContactsDto(Contacts contacts) {
        this.aggregateId = contacts.getAggregateId();
        this.version = contacts.getVersion();
        this.state = contacts.getState();
        this.userAggregateId = contacts.getUserAggregateId();
        this.name = contacts.getName();
        this.documentType = contacts.getDocumentType();
        this.documentNumber = contacts.getDocumentNumber();
        this.phoneNumber = contacts.getPhoneNumber();
    }

    public boolean isActive() {
        return this.state == Aggregate.AggregateState.ACTIVE;
    }

    public Integer getAggregateId() {
        return this.aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Aggregate.AggregateState getState() {
        return this.state;
    }

    public void setState(Aggregate.AggregateState state) {
        this.state = state;
    }

    public Integer getUserAggregateId() {
        return this.userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
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
