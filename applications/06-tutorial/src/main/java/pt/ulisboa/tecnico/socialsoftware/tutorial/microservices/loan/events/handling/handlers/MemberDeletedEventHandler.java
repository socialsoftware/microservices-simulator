package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.LoanRepository;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.eventProcessing.LoanEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.MemberDeletedEvent;

public class MemberDeletedEventHandler extends LoanEventHandler {
    public MemberDeletedEventHandler(LoanRepository loanRepository, LoanEventProcessing loanEventProcessing) {
        super(loanRepository, loanEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.loanEventProcessing.processMemberDeletedEvent(subscriberAggregateId, (MemberDeletedEvent) event);
    }
}
