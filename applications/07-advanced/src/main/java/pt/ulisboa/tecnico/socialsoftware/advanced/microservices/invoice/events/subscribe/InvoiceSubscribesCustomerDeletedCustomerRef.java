package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.InvoiceCustomer;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.CustomerDeletedEvent;


public class InvoiceSubscribesCustomerDeletedCustomerRef extends EventSubscription {
    public InvoiceSubscribesCustomerDeletedCustomerRef(InvoiceCustomer customer) {
        super(customer.getCustomerAggregateId(),
                customer.getCustomerVersion(),
                CustomerDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
