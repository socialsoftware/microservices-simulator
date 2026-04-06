package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;
import jakarta.persistence.Column;

@Entity
public class ExecutionUpdatedEvent extends Event {
    @Column(name = "execution_updated_event_acronym")
    private String acronym;
    @Column(name = "execution_updated_event_academic_term")
    private String academicTerm;
    @Column(name = "execution_updated_event_end_date")
    private LocalDateTime endDate;

    public ExecutionUpdatedEvent() {
        super();
    }

    public ExecutionUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
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