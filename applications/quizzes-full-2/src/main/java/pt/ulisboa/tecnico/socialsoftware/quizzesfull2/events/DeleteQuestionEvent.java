package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class DeleteQuestionEvent extends Event {
    private Integer questionAggregateId;
    private Integer courseAggregateId;

    protected DeleteQuestionEvent() {}

    public DeleteQuestionEvent(Integer questionAggregateId, Integer courseAggregateId) {
        super(questionAggregateId);
        this.questionAggregateId = questionAggregateId;
        this.courseAggregateId = courseAggregateId;
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }
}
