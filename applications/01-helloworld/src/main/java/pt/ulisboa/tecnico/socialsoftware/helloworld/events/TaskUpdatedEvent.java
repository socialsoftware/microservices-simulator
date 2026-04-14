package pt.ulisboa.tecnico.socialsoftware.helloworld.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class TaskUpdatedEvent extends Event {
    @Column(name = "task_updated_event_title")
    private String title;
    @Column(name = "task_updated_event_description")
    private String description;
    @Column(name = "task_updated_event_done")
    private Boolean done;

    public TaskUpdatedEvent() {
        super();
    }

    public TaskUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TaskUpdatedEvent(Integer aggregateId, String title, String description, Boolean done) {
        super(aggregateId);
        setTitle(title);
        setDescription(description);
        setDone(done);
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

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

}