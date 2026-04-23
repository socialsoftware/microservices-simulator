package pt.ulisboa.tecnico.socialsoftware.crossrefs.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TeacherDeletedEvent extends Event {
    private String name;

    public TeacherDeletedEvent() {
        super();
    }

    public TeacherDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TeacherDeletedEvent(Integer aggregateId, String name) {
        super(aggregateId);
        setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}