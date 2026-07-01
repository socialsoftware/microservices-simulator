package pt.ulisboa.tecnico.socialsoftware.quizzes.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class DeleteCourseExecutionEvent extends Event {
    public DeleteCourseExecutionEvent() {}

    public DeleteCourseExecutionEvent(Integer aggregateId) {
        super(aggregateId);
    }
}
