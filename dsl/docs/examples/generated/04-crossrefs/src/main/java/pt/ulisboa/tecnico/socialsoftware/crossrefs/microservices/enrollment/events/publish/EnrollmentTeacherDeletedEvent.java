package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class EnrollmentTeacherDeletedEvent extends Event {
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