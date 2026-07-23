package pt.ulisboa.tecnico.socialsoftware.quizzesfull.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class UpdateStudentNameEvent extends Event {
    private Integer studentAggregateId;
    private String updatedName;

    public UpdateStudentNameEvent() {}

    public UpdateStudentNameEvent(Integer userAggregateId, String updatedName) {
        super(userAggregateId);
        this.studentAggregateId = userAggregateId;
        this.updatedName = updatedName;
    }

    public Integer getStudentAggregateId() { return studentAggregateId; }
    public void setStudentAggregateId(Integer studentAggregateId) { this.studentAggregateId = studentAggregateId; }
    public String getUpdatedName() { return updatedName; }
    public void setUpdatedName(String updatedName) { this.updatedName = updatedName; }
}
