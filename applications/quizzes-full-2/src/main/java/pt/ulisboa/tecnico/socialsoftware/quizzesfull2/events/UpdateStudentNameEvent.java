package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class UpdateStudentNameEvent extends Event {
    private Integer studentAggregateId;
    private String updatedName;

    protected UpdateStudentNameEvent() {}

    public UpdateStudentNameEvent(Integer studentAggregateId, String updatedName) {
        super(studentAggregateId);
        this.studentAggregateId = studentAggregateId;
        this.updatedName = updatedName;
    }

    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }

    public String getUpdatedName() {
        return updatedName;
    }
}
