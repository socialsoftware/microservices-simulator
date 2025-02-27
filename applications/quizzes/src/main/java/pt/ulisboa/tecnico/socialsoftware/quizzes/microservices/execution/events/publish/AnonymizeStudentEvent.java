package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class AnonymizeStudentEvent extends Event {
    private String name;
    private String username;
    private Integer studentAggregateId;

    public AnonymizeStudentEvent() {
    }

    public AnonymizeStudentEvent(Integer aggregateId, String name, String username, Integer userAggregateId) {
        super(aggregateId);
        setName(name);
        setUsername(username);
        setStudentAggregateId(userAggregateId);
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

    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }

    public void setStudentAggregateId(Integer executionAggregateId) {
        this.studentAggregateId = executionAggregateId;
    }
}
