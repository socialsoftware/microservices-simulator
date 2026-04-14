package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.Invoice;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserDeletedEvent;

public class InvoiceSubscribesUserDeleted extends EventSubscription {
    public InvoiceSubscribesUserDeleted(Invoice invoice) {
        super(invoice.getAggregateId(), 0, UserDeletedEvent.class.getSimpleName());
    }
}
