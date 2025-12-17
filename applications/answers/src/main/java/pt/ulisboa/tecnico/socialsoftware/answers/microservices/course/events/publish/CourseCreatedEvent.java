package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CourseCreatedEvent extends Event {
    private String name;
    private CourseType type;
    private LocalDateTime creationDate;

    public CourseCreatedEvent() {
    }

    public CourseCreatedEvent(Integer aggregateId, String name, CourseType type, LocalDateTime creationDate) {
        super(aggregateId);
        setName(name);
        setType(type);
        setCreationDate(creationDate);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CourseType getType() {
        return type;
    }

    public void setType(CourseType type) {
        this.type = type;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

}