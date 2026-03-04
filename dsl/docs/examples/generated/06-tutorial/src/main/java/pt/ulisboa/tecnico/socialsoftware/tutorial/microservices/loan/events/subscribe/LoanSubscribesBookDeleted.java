package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.Loan;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.BookDeletedEvent;

public class LoanSubscribesBookDeleted extends EventSubscription {
    public LoanSubscribesBookDeleted(Loan loan) {
        super(loan.getAggregateId(), 0, BookDeletedEvent.class.getSimpleName());
    }
}
