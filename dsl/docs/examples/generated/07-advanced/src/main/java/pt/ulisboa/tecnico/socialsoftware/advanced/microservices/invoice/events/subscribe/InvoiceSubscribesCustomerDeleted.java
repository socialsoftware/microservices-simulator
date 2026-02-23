package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.Invoice;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.events.publish.CustomerDeletedEvent;

public class InvoiceSubscribesCustomerDeleted extends EventSubscription {
    public InvoiceSubscribesCustomerDeleted(Invoice invoice) {
        super(invoice.getAggregateId(), 0, CustomerDeletedEvent.class);
    }
}
