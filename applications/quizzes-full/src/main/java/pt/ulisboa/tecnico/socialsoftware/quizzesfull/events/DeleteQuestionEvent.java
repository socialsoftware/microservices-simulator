package pt.ulisboa.tecnico.socialsoftware.quizzesfull.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class DeleteQuestionEvent extends Event {

    private Integer courseAggregateId;

    public DeleteQuestionEvent() {
        super();
    }

    public DeleteQuestionEvent(Integer questionAggregateId, Integer courseAggregateId) {
        super(questionAggregateId);
        this.courseAggregateId = courseAggregateId;
    }

    public Integer getCourseAggregateId() { return courseAggregateId; }
    public void setCourseAggregateId(Integer courseAggregateId) { this.courseAggregateId = courseAggregateId; }
}
