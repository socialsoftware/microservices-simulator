package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;

@Entity
public class CourseUpdatedEvent extends Event {
    private String name;
    private LocalDateTime creationDate;

    public CourseUpdatedEvent() {
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