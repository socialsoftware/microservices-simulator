package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate;

import java.util.HashSet;
import java.util.Set;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;

import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos.ContactDto;
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.enums.ContactCategory;

@Entity
public abstract class Contact extends Aggregate {
    private String firstName;
    private String lastName;
    private String email;
    @Enumerated(EnumType.STRING)
    private ContactCategory category;
    private LocalDateTime createdAt;
    private Boolean favorite;
    private Integer callCount;

    public Contact() {

    }

    public Contact(Integer aggregateId, ContactDto contactDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setFirstName(contactDto.getFirstName());
        setLastName(contactDto.getLastName());
        setEmail(contactDto.getEmail());
        setCategory(ContactCategory.valueOf(contactDto.getCategory()));
        setCreatedAt(contactDto.getCreatedAt());
        setFavorite(contactDto.getFavorite());
        setCallCount(contactDto.getCallCount());
    }


    public Contact(Contact other) {
        super(other);
        setFirstName(other.getFirstName());
        setLastName(other.getLastName());
        setEmail(other.getEmail());
        setCategory(other.getCategory());
        setCreatedAt(other.getCreatedAt());
        setFavorite(other.getFavorite());
        setCallCount(other.getCallCount());
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

    public ContactCategory getCategory() {
        return category;
    }

    public void setCategory(ContactCategory category) {
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }


    @Override
    public void verifyInvariants() {
    }

    public ContactDto buildDto() {
        ContactDto dto = new ContactDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setFirstName(getFirstName());
        dto.setLastName(getLastName());
        dto.setEmail(getEmail());
        dto.setCategory(getCategory() != null ? getCategory().name() : null);
        dto.setCreatedAt(getCreatedAt());
        dto.setFavorite(getFavorite());
        dto.setCallCount(getCallCount());
        return dto;
    }
}