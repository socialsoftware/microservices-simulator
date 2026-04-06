package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class QuestionCourseDeletedEvent extends Event {
    @Column(name = "question_course_deleted_event_course_aggregate_id")
    private Integer courseAggregateId;

    public QuestionCourseDeletedEvent() {
        super();
    }

    public QuestionCourseDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public QuestionCourseDeletedEvent(Integer aggregateId, Integer courseAggregateId) {
        super(aggregateId);
        setCourseAggregateId(courseAggregateId);
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }

}