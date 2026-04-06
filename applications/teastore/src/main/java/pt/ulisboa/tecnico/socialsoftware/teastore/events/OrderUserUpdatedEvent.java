package pt.ulisboa.tecnico.socialsoftware.teastore.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class OrderUserUpdatedEvent extends Event {
    @Column(name = "order_user_updated_event_user_aggregate_id")
    private Integer userAggregateId;
    @Column(name = "order_user_updated_event_user_version")
    private Integer userVersion;
    @Column(name = "order_user_updated_event_user_name")
    private String userName;
    @Column(name = "order_user_updated_event_user_real_name")
    private String userRealName;
    @Column(name = "order_user_updated_event_user_email")
    private String userEmail;

    public OrderUserUpdatedEvent() {
        super();
    }

    public OrderUserUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderUserUpdatedEvent(Integer aggregateId, Integer userAggregateId, Integer userVersion, String userName, String userRealName, String userEmail) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
        setUserVersion(userVersion);
        setUserName(userName);
        setUserRealName(userRealName);
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

    public String getUserRealName() {
        return userRealName;
    }

    public void setUserRealName(String userRealName) {
        this.userRealName = userRealName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

}