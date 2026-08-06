package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.DocumentType;

@Entity
public class OrderContacts {
    @Id
    @GeneratedValue
    private Long id;
    private String contactsName;
    @Enumerated(EnumType.STRING)
    private DocumentType contactsDocumentType;
    private String contactsDocumentNumber;
    private Integer contactsAggregateId;
    private Long contactsVersion;
    private AggregateState contactsState;
    @OneToOne
    private Order order;

    public OrderContacts() {

    }

    public OrderContacts(ContactsDto contactsDto) {
        setContactsAggregateId(contactsDto.getAggregateId());
        setContactsVersion(contactsDto.getVersion());
        setContactsState(contactsDto.getState());
    }

    public OrderContacts(OrderContactsDto orderContactsDto) {
        setContactsName(orderContactsDto.getName());
        setContactsDocumentType(orderContactsDto.getDocumentType() != null ? DocumentType.valueOf(orderContactsDto.getDocumentType()) : null);
        setContactsDocumentNumber(orderContactsDto.getDocumentNumber());
        setContactsAggregateId(orderContactsDto.getAggregateId());
        setContactsVersion(orderContactsDto.getVersion());
        setContactsState(orderContactsDto.getState() != null ? AggregateState.valueOf(orderContactsDto.getState()) : null);
    }

    public OrderContacts(OrderContacts other) {
        setContactsName(other.getContactsName());
        setContactsDocumentType(other.getContactsDocumentType());
        setContactsDocumentNumber(other.getContactsDocumentNumber());
        setContactsAggregateId(other.getContactsAggregateId());
        setContactsVersion(other.getContactsVersion());
        setContactsState(other.getContactsState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContactsName() {
        return contactsName;
    }

    public void setContactsName(String contactsName) {
        this.contactsName = contactsName;
    }

    public DocumentType getContactsDocumentType() {
        return contactsDocumentType;
    }

    public void setContactsDocumentType(DocumentType contactsDocumentType) {
        this.contactsDocumentType = contactsDocumentType;
    }

    public String getContactsDocumentNumber() {
        return contactsDocumentNumber;
    }

    public void setContactsDocumentNumber(String contactsDocumentNumber) {
        this.contactsDocumentNumber = contactsDocumentNumber;
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

    public AggregateState getContactsState() {
        return contactsState;
    }

    public void setContactsState(AggregateState contactsState) {
        this.contactsState = contactsState;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }




    public OrderContactsDto buildDto() {
        OrderContactsDto dto = new OrderContactsDto();
        dto.setName(getContactsName());
        dto.setDocumentType(getContactsDocumentType() != null ? getContactsDocumentType().name() : null);
        dto.setDocumentNumber(getContactsDocumentNumber());
        dto.setAggregateId(getContactsAggregateId());
        dto.setVersion(getContactsVersion());
        dto.setState(getContactsState() != null ? getContactsState().name() : null);
        return dto;
    }
}