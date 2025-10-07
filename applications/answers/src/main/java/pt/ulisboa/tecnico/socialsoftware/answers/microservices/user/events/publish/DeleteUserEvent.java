package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class DeleteUserEvent extends Event {
    private Integer userAggregateId;
    private String username;

    public DeleteUserEvent() {
    }

    public DeleteUserEvent(Integer aggregateId, Integer userAggregateId, String username) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
        setUsername(username);
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}