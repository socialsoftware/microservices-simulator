package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class AnonymizeUserEvent extends Event {
    private String name;
    private String username;
    private Integer userAggregateId;

    public AnonymizeUserEvent() {
    }

    public AnonymizeUserEvent(Integer aggregateId, String name, String username, Integer userAggregateId) {
        super(aggregateId);
        setName(name);
        setUsername(username);
        setUserAggregateId(userAggregateId);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

}