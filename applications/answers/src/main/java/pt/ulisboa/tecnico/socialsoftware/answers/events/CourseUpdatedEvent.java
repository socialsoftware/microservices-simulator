package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;
import jakarta.persistence.Column;

@Entity
public class CourseUpdatedEvent extends Event {
    @Column(name = "course_updated_event_name")
    private String name;
    @Column(name = "course_updated_event_creation_date")
    private LocalDateTime creationDate;

    public CourseUpdatedEvent() {
        super();
    }

    public CourseUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public CourseUpdatedEvent(Integer aggregateId, String name, LocalDateTime creationDate) {
        super(aggregateId);
        setName(name);
        setCreationDate(creationDate);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

}