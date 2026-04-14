package pt.ulisboa.tecnico.socialsoftware.crossrefs.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class CourseTeacherDeletedEvent extends Event {
    @Column(name = "course_teacher_deleted_event_teacher_aggregate_id")
    private Integer teacherAggregateId;

    public CourseTeacherDeletedEvent() {
        super();
    }

    public CourseTeacherDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public CourseTeacherDeletedEvent(Integer aggregateId, Integer teacherAggregateId) {
        super(aggregateId);
        setTeacherAggregateId(teacherAggregateId);
    }

    public Integer getTeacherAggregateId() {
        return teacherAggregateId;
    }

    public void setTeacherAggregateId(Integer teacherAggregateId) {
        this.teacherAggregateId = teacherAggregateId;
    }

}