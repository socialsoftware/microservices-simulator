package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class ExecutionUserUpdatedEvent extends Event {
    @Column(name = "execution_user_updated_event_user_aggregate_id")
    private Integer userAggregateId;
    @Column(name = "execution_user_updated_event_user_version")
    private Integer userVersion;
    @Column(name = "execution_user_updated_event_user_name")
    private String userName;
    @Column(name = "execution_user_updated_event_user_username")
    private String userUsername;
    @Column(name = "execution_user_updated_event_user_active")
    private Boolean userActive;

    public ExecutionUserUpdatedEvent() {
        super();
    }

    public ExecutionUserUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ExecutionUserUpdatedEvent(Integer aggregateId, Integer userAggregateId, Integer userVersion, String userName, String userUsername, Boolean userActive) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
        setUserVersion(userVersion);
        setUserName(userName);
        setUserUsername(userUsername);
        setUserActive(userActive);
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

    public String getUserUsername() {
        return userUsername;
    }

    public void setUserUsername(String userUsername) {
        this.userUsername = userUsername;
    }

    public Boolean getUserActive() {
        return userActive;
    }

    public void setUserActive(Boolean userActive) {
        this.userActive = userActive;
    }

}