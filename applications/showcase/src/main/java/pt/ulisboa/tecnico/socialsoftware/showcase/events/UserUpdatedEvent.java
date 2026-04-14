package pt.ulisboa.tecnico.socialsoftware.showcase.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class UserUpdatedEvent extends Event {
    @Column(name = "user_updated_event_username")
    private String username;
    @Column(name = "user_updated_event_email")
    private String email;
    @Column(name = "user_updated_event_loyalty_points")
    private Integer loyaltyPoints;

    public UserUpdatedEvent() {
        super();
    }

    public UserUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public UserUpdatedEvent(Integer aggregateId, String username, String email, Integer loyaltyPoints) {
        super(aggregateId);
        setUsername(username);
        setEmail(email);
        setLoyaltyPoints(loyaltyPoints);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(Integer loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

}