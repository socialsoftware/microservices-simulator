package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class AnswerUserUpdatedEvent extends Event {
    @Column(name = "answer_user_updated_event_user_aggregate_id")
    private Integer userAggregateId;
    @Column(name = "answer_user_updated_event_user_version")
    private Integer userVersion;
    @Column(name = "answer_user_updated_event_user_name")
    private String userName;

    public AnswerUserUpdatedEvent() {
        super();
    }

    public AnswerUserUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public AnswerUserUpdatedEvent(Integer aggregateId, Integer userAggregateId, Integer userVersion, String userName) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
        setUserVersion(userVersion);
        setUserName(userName);
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

}