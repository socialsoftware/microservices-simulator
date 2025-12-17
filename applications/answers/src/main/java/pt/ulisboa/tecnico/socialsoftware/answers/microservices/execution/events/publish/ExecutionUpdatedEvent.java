package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ExecutionUpdatedEvent extends Event {
    private String acronym;
    private String academicTerm;
    private LocalDateTime endDate;

    public ExecutionUpdatedEvent() {
    }

    public ExecutionUpdatedEvent(Integer aggregateId, String acronym, String academicTerm, LocalDateTime endDate) {
        super(aggregateId);
        setAcronym(acronym);
        setAcademicTerm(academicTerm);
        setEndDate(endDate);
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

}