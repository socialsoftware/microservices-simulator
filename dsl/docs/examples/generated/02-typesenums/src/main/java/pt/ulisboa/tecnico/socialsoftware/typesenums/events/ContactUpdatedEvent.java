package pt.ulisboa.tecnico.socialsoftware.typesenums.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;
import jakarta.persistence.Column;

@Entity
public class ContactUpdatedEvent extends Event {
    @Column(name = "contact_updated_event_first_name")
    private String firstName;
    @Column(name = "contact_updated_event_last_name")
    private String lastName;
    @Column(name = "contact_updated_event_email")
    private String email;
    @Column(name = "contact_updated_event_created_at")
    private LocalDateTime createdAt;
    @Column(name = "contact_updated_event_favorite")
    private Boolean favorite;
    @Column(name = "contact_updated_event_call_count")
    private Integer callCount;

    public ContactUpdatedEvent() {
        super();
    }

    public ContactUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ContactUpdatedEvent(Integer aggregateId, String firstName, String lastName, String email, LocalDateTime createdAt, Boolean favorite, Integer callCount) {
        super(aggregateId);
        setFirstName(firstName);
        setLastName(lastName);
        setEmail(email);
        setCreatedAt(createdAt);
        setFavorite(favorite);
        setCallCount(callCount);
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