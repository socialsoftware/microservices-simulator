package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class DeleteCourseExecutionEvent extends Event {
    private Integer executionAggregateId;

    protected DeleteCourseExecutionEvent() {}

    public DeleteCourseExecutionEvent(Integer executionAggregateId) {
        super(executionAggregateId);
        this.executionAggregateId = executionAggregateId;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }
}
