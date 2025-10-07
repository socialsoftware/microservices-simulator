package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class UpdateStudentNameEvent extends Event {
    private String oldName;
    private String newName;
    private Integer studentAggregateId;

    public UpdateStudentNameEvent() {
    }

    public UpdateStudentNameEvent(Integer aggregateId, String oldName, String newName, Integer studentAggregateId) {
        super(aggregateId);
        setOldName(oldName);
        setNewName(newName);
        setStudentAggregateId(studentAggregateId);
    }

    public String getOldName() {
        return oldName;
    }

    public void setOldName(String oldName) {
        this.oldName = oldName;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }

    public void setStudentAggregateId(Integer studentAggregateId) {
        this.studentAggregateId = studentAggregateId;
    }

}