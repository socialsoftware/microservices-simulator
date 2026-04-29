package pt.ulisboa.tecnico.socialsoftware.quizzesfull.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class AnonymizeStudentEvent extends Event {
    private Integer studentAggregateId;
    private String name;
    private String username;

    public AnonymizeStudentEvent() {}

    public AnonymizeStudentEvent(Integer userAggregateId, String name, String username) {
        super(userAggregateId);
        this.studentAggregateId = userAggregateId;
        this.name = name;
        this.username = username;
    }

    public Integer getStudentAggregateId() { return studentAggregateId; }
    public void setStudentAggregateId(Integer studentAggregateId) { this.studentAggregateId = studentAggregateId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
