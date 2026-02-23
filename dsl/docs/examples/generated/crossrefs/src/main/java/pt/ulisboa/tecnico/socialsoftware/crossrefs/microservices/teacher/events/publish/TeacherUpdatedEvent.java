package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TeacherUpdatedEvent extends Event {
    private String name;
    private String email;
    private String department;

    public TeacherUpdatedEvent() {
        super();
    }

    public TeacherUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TeacherUpdatedEvent(Integer aggregateId, String name, String email, String department) {
        super(aggregateId);
        setName(name);
        setEmail(email);
        setDepartment(department);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

}