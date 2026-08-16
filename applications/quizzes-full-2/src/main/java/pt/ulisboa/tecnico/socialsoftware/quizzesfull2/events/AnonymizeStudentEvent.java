package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class AnonymizeStudentEvent extends Event {
    private Integer studentAggregateId;
    private String name;
    private String username;

    protected AnonymizeStudentEvent() {}

    public AnonymizeStudentEvent(Integer studentAggregateId, String name, String username) {
        super(studentAggregateId);
        this.studentAggregateId = studentAggregateId;
        this.name = name;
        this.username = username;
    }

    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }
}
