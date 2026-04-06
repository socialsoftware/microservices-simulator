package pt.ulisboa.tecnico.socialsoftware.crossrefs.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class EnrollmentTeacherRemovedEvent extends Event {
    @Column(name = "enrollment_teacher_removed_event_teacher_aggregate_id")
    private Integer teacherAggregateId;

    public EnrollmentTeacherRemovedEvent() {
        super();
    }

    public EnrollmentTeacherRemovedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public EnrollmentTeacherRemovedEvent(Integer aggregateId, Integer teacherAggregateId) {
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