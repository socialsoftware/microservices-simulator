package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class DisenrollStudentFromCourseExecutionEvent extends Event {
    private Integer studentAggregateId;

    public DisenrollStudentFromCourseExecutionEvent() {
        super();
    }

    public DisenrollStudentFromCourseExecutionEvent(Integer courseExecutionAggregateId, Integer studentAggregateId) {
        super(courseExecutionAggregateId);
        setStudentAggregateId(studentAggregateId);
    }

    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }

    public void setStudentAggregateId(Integer studentAggregateId) {
        this.studentAggregateId = studentAggregateId;
    }
}
