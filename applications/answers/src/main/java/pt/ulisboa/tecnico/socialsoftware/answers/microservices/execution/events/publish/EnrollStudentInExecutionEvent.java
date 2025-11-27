package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class EnrollStudentInExecutionEvent extends Event {
    private Integer executionAggregateId;
    private Integer studentAggregateId;
    private String studentName;

    public EnrollStudentInExecutionEvent() {
    }

    public EnrollStudentInExecutionEvent(Integer aggregateId, Integer executionAggregateId, Integer studentAggregateId, String studentName) {
        super(aggregateId);
        setExecutionAggregateId(executionAggregateId);
        setStudentAggregateId(studentAggregateId);
        setStudentName(studentName);
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }

    public void setStudentAggregateId(Integer studentAggregateId) {
        this.studentAggregateId = studentAggregateId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

}