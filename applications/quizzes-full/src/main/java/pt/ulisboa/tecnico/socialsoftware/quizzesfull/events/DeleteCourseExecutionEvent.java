package pt.ulisboa.tecnico.socialsoftware.quizzesfull.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class DeleteCourseExecutionEvent extends Event {
    public DeleteCourseExecutionEvent() {
        super();
    }

    public DeleteCourseExecutionEvent(Integer executionAggregateId) {
        super(executionAggregateId);
    }
}
