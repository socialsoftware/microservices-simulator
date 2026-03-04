package pt.ulisboa.tecnico.socialsoftware.crossrefs.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;

@Entity
public class EnrollmentUpdatedEvent extends Event {
    private LocalDateTime enrollmentDate;
    private Boolean active;

    public EnrollmentUpdatedEvent() {
        super();
    }

    public EnrollmentUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public EnrollmentUpdatedEvent(Integer aggregateId, LocalDateTime enrollmentDate, Boolean active) {
        super(aggregateId);
        setEnrollmentDate(enrollmentDate);
        setActive(active);
    }

    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

}