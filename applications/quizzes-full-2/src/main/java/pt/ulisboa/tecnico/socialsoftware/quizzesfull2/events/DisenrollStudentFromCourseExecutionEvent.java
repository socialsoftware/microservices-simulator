package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class DisenrollStudentFromCourseExecutionEvent extends Event {
    private Integer executionAggregateId;
    private Integer studentAggregateId;

    protected DisenrollStudentFromCourseExecutionEvent() {}

    public DisenrollStudentFromCourseExecutionEvent(Integer executionAggregateId, Integer studentAggregateId) {
        super(executionAggregateId);
        this.executionAggregateId = executionAggregateId;
        this.studentAggregateId = studentAggregateId;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }
}
