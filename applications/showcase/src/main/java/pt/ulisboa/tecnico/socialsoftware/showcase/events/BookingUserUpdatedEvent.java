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
    @Column(name = "booking_user_updated_event_user_name")
    private String userName;
    @Column(name = "booking_user_updated_event_user_email")
    private String userEmail;

    public BookingUserUpdatedEvent() {
        super();
    }

    public BookingUserUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public BookingUserUpdatedEvent(Integer aggregateId, Integer userAggregateId, Integer userVersion, String userName, String userEmail) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
        setUserVersion(userVersion);
        setUserName(userName);
        setUserEmail(userEmail);
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

}