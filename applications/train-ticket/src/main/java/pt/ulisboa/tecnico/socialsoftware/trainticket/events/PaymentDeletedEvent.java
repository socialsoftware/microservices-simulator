package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class PaymentDeletedEvent extends Event {

    public PaymentDeletedEvent() {
        super();
    }

    public PaymentDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}
