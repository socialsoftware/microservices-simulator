package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class PaymentDeletedEvent extends Event {

    public PaymentDeletedEvent() {
        super();
    }

    public PaymentDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}