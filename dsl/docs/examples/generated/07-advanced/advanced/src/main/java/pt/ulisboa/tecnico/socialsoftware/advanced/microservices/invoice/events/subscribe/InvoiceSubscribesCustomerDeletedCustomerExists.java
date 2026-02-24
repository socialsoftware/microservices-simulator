package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.InvoiceCustomer;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.CustomerDeletedEvent;


public class InvoiceSubscribesCustomerDeletedCustomerExists extends EventSubscription {
    public InvoiceSubscribesCustomerDeletedCustomerExists(InvoiceCustomer customer) {
        super(customer.getCustomerAggregateId(),
                customer.getCustomerVersion(),
                CustomerDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
