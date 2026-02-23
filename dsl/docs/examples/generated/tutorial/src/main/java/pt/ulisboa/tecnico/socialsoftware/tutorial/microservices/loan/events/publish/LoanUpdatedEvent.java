package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;

@Entity
public class LoanUpdatedEvent extends Event {
    private LocalDateTime loanDate;
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