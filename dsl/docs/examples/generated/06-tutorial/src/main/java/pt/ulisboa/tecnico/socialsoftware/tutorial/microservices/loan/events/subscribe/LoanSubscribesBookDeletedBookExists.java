package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.LoanBook;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.BookDeletedEvent;


public class LoanSubscribesBookDeletedBookExists extends EventSubscription {
    public LoanSubscribesBookDeletedBookExists(LoanBook book) {
        super(book.getBookAggregateId(),
                book.getBookVersion(),
                BookDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
