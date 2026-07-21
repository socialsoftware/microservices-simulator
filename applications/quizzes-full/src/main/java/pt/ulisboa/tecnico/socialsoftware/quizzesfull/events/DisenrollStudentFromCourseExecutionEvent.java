package pt.ulisboa.tecnico.socialsoftware.quizzesfull.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class DisenrollStudentFromCourseExecutionEvent extends Event {
    private Integer userId;

    public DisenrollStudentFromCourseExecutionEvent() {
        super();
    }

    public DisenrollStudentFromCourseExecutionEvent(Integer executionAggregateId, Integer userId) {
        super(executionAggregateId);
        this.userId = userId;
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
}
