package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderUserUpdatedEvent extends Event {
    private Integer userAggregateId;
    private Integer userVersion;
    private String userName;
    private String userRealName;
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