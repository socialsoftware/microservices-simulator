package pt.ulisboa.tecnico.socialsoftware.crossrefs.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class EnrollmentTeacherUpdatedEvent extends Event {
    @Column(name = "enrollment_teacher_updated_event_teacher_aggregate_id")
    private Integer teacherAggregateId;
    @Column(name = "enrollment_teacher_updated_event_teacher_version")
    private Integer teacherVersion;

    public EnrollmentTeacherUpdatedEvent() {
        super();
    }

    public EnrollmentTeacherUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public EnrollmentTeacherUpdatedEvent(Integer aggregateId, Integer teacherAggregateId, Integer teacherVersion) {
        super(aggregateId);
        setTeacherAggregateId(teacherAggregateId);
        setTeacherVersion(teacherVersion);
    }

    public Integer getTeacherAggregateId() {
        return teacherAggregateId;
    }

    public void setTeacherAggregateId(Integer teacherAggregateId) {
        this.teacherAggregateId = teacherAggregateId;
    }

    public Integer getTeacherVersion() {
        return teacherVersion;
    }

    public void setTeacherVersion(Integer teacherVersion) {
        this.teacherVersion = teacherVersion;
    }

}