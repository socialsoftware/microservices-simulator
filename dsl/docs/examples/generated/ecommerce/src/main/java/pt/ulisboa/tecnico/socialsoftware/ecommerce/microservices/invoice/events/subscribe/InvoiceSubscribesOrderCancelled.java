package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.Invoice;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderCancelledEvent;

public class InvoiceSubscribesOrderCancelled extends EventSubscription {
    

    public InvoiceSubscribesOrderCancelled(Invoice invoice) {
        super(invoice.getAggregateId(),
                0,
                OrderCancelledEvent.class.getSimpleName());
        
    }

    public InvoiceSubscribesOrderCancelled() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
