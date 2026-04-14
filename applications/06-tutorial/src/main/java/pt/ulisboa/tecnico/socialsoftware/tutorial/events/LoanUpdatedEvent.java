package pt.ulisboa.tecnico.socialsoftware.tutorial.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;
import jakarta.persistence.Column;

@Entity
public class LoanUpdatedEvent extends Event {
    @Column(name = "loan_updated_event_loan_date")
    private LocalDateTime loanDate;
    @Column(name = "loan_updated_event_due_date")
    private LocalDateTime dueDate;

    public LoanUpdatedEvent() {
        super();
    }

    public LoanUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public LoanUpdatedEvent(Integer aggregateId, LocalDateTime loanDate, LocalDateTime dueDate) {
        super(aggregateId);
        setLoanDate(loanDate);
        setDueDate(dueDate);
    }

    public LocalDateTime getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(LocalDateTime loanDate) {
        this.loanDate = loanDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

}