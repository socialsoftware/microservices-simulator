package pt.ulisboa.tecnico.socialsoftware.crossrefs.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class EnrollmentTeacherDeletedEvent extends Event {
    @Column(name = "enrollment_teacher_deleted_event_teacher_aggregate_id")
    private Integer teacherAggregateId;

    public EnrollmentTeacherDeletedEvent() {
        super();
    }

    public EnrollmentTeacherDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public EnrollmentTeacherDeletedEvent(Integer aggregateId, Integer teacherAggregateId) {
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