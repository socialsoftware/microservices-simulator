package pt.ulisboa.tecnico.socialsoftware.crossrefs.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class CourseUpdatedEvent extends Event {
    @Column(name = "course_updated_event_title")
    private String title;
    @Column(name = "course_updated_event_description")
    private String description;
    @Column(name = "course_updated_event_max_students")
    private Integer maxStudents;

    public CourseUpdatedEvent() {
        super();
    }

    public CourseUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public CourseUpdatedEvent(Integer aggregateId, String title, String description, Integer maxStudents) {
        super(aggregateId);
        setTitle(title);
        setDescription(description);
        setMaxStudents(maxStudents);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMaxStudents() {
        return maxStudents;
    }

    public void setMaxStudents(Integer maxStudents) {
        this.maxStudents = maxStudents;
    }

}