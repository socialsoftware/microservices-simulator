package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.LoanRepository;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.eventProcessing.LoanEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.BookDeletedEvent;

public class BookDeletedEventHandler extends LoanEventHandler {
    public BookDeletedEventHandler(LoanRepository loanRepository, LoanEventProcessing loanEventProcessing) {
        super(loanRepository, loanEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.loanEventProcessing.processBookDeletedEvent(subscriberAggregateId, (BookDeletedEvent) event);
    }
}
