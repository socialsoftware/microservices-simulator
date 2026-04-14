package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.Loan;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.MemberDeletedEvent;

public class LoanSubscribesMemberDeleted extends EventSubscription {
    public LoanSubscribesMemberDeleted(Loan loan) {
        super(loan.getAggregateId(), 0, MemberDeletedEvent.class.getSimpleName());
    }
}
