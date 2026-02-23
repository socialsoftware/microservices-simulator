package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.Invoice;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish.OrderDeletedEvent;

public class InvoiceSubscribesOrderDeleted extends EventSubscription {
    public InvoiceSubscribesOrderDeleted(Invoice invoice) {
        super(invoice.getAggregateId(), 0, OrderDeletedEvent.class);
    }
}
