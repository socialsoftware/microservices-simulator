package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class LoanBookDeletedEvent extends Event {
    private Integer bookAggregateId;

    public LoanBookDeletedEvent() {
        super();
    }

    public LoanBookDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public LoanBookDeletedEvent(Integer aggregateId, Integer bookAggregateId) {
        super(aggregateId);
        setBookAggregateId(bookAggregateId);
    }

    public Integer getBookAggregateId() {
        return bookAggregateId;
    }

    public void setBookAggregateId(Integer bookAggregateId) {
        this.bookAggregateId = bookAggregateId;
    }

}