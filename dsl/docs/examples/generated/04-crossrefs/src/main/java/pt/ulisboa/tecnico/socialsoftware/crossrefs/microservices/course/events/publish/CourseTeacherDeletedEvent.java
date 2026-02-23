package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CourseTeacherDeletedEvent extends Event {
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