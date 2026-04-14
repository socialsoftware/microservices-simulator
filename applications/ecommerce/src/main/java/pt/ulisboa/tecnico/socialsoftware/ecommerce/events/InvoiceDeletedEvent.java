package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class InvoiceDeletedEvent extends Event {

    public InvoiceDeletedEvent() {
        super();
    }

    public InvoiceDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}