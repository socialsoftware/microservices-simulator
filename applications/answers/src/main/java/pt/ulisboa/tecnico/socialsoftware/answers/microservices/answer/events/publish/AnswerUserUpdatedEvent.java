package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class AnswerUserUpdatedEvent extends Event {
    private Integer userAggregateId;
    private Integer userVersion;
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