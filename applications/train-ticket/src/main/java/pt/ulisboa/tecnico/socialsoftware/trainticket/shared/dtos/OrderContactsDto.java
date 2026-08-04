package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderContacts;

public class OrderContactsDto implements Serializable {
    private String name;
    private String documentType;
    private String documentNumber;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public OrderContactsDto() {
    }

    public OrderContactsDto(OrderContacts orderContacts) {
        this.name = orderContacts.getContactsName();
        this.documentType = orderContacts.getContactsDocumentType() != null ? orderContacts.getContactsDocumentType().name() : null;
        this.documentNumber = orderContacts.getContactsDocumentNumber();
        this.aggregateId = orderContacts.getContactsAggregateId();
        this.version = orderContacts.getContactsVersion();
        this.state = orderContacts.getContactsState() != null ? orderContacts.getContactsState().name() : null;
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