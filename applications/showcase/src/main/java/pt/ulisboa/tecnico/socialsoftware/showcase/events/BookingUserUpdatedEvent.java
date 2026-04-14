package pt.ulisboa.tecnico.socialsoftware.showcase.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class BookingUserUpdatedEvent extends Event {
    @Column(name = "booking_user_updated_event_user_aggregate_id")
    private Integer userAggregateId;
    @Column(name = "booking_user_updated_event_user_version")
    private Integer userVersion;
    @Column(name = "booking_user_updated_event_username")
    private String username;
    @Column(name = "booking_user_updated_event_email")
    private String email;

    public BookingUserUpdatedEvent() {
        super();
    }

    public BookingUserUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public BookingUserUpdatedEvent(Integer aggregateId, Integer userAggregateId, Integer userVersion, String username, String email) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
        setUserVersion(userVersion);
        setUsername(username);
        setEmail(email);
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Integer getUserVersion() {
        return userVersion;
    }

    public void setUserVersion(Integer userVersion) {
        this.userVersion = userVersion;
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

}