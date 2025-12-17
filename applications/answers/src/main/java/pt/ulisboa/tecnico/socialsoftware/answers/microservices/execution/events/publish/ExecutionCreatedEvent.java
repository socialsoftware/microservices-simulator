package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ExecutionCreatedEvent extends Event {
    private String acronym;
    private String academicTerm;
    private LocalDateTime endDate;
    private ExecutionCourse course;
    private Set<ExecutionUser> users;

    public ExecutionCreatedEvent() {
    }

    public ExecutionCreatedEvent(Integer aggregateId, String acronym, String academicTerm, LocalDateTime endDate, ExecutionCourse course, Set<ExecutionUser> users) {
        super(aggregateId);
        setAcronym(acronym);
        setAcademicTerm(academicTerm);
        setEndDate(endDate);
        setCourse(course);
        setUsers(users);
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getAcademicTerm() {
        return academicTerm;
    }

    public void setAcademicTerm(String academicTerm) {
        this.academicTerm = academicTerm;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public ExecutionCourse getCourse() {
        return course;
    }

    public void setCourse(ExecutionCourse course) {
        this.course = course;
    }

    public Set<ExecutionUser> getUsers() {
        return users;
    }

    public void setUsers(Set<ExecutionUser> users) {
        this.users = users;
    }

}