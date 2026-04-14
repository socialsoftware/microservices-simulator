package pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.Contact;

public class ContactDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String firstName;
    private String lastName;
    private String email;
    private String category;
    private LocalDateTime createdAt;
    private Boolean favorite;
    private Integer callCount;

    public ContactDto() {
    }

    public ContactDto(Contact contact) {
        this.aggregateId = contact.getAggregateId();
        this.version = contact.getVersion();
        this.state = contact.getState();
        this.firstName = contact.getFirstName();
        this.lastName = contact.getLastName();
        this.email = contact.getEmail();
        this.category = contact.getCategory() != null ? contact.getCategory().name() : null;
        this.createdAt = contact.getCreatedAt();
        this.favorite = contact.getFavorite();
        this.callCount = contact.getCallCount();
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(Boolean favorite) {
        this.favorite = favorite;
    }

    public Integer getCallCount() {
        return callCount;
    }

    public void setCallCount(Integer callCount) {
        this.callCount = callCount;
    }
}