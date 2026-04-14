package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class CartUserUpdatedEvent extends Event {
    @Column(name = "cart_user_updated_event_user_aggregate_id")
    private Integer userAggregateId;
    @Column(name = "cart_user_updated_event_user_version")
    private Integer userVersion;
    @Column(name = "cart_user_updated_event_user_name")
    private String userName;
    @Column(name = "cart_user_updated_event_user_email")
    private String userEmail;

    public CartUserUpdatedEvent() {
        super();
    }

    public CartUserUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public CartUserUpdatedEvent(Integer aggregateId, Integer userAggregateId, Integer userVersion, String userName, String userEmail) {
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