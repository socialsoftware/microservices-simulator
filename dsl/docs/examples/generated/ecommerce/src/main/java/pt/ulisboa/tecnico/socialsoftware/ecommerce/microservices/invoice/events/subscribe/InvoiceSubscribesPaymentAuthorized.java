package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.Invoice;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.PaymentAuthorizedEvent;

public class InvoiceSubscribesPaymentAuthorized extends EventSubscription {
    

    public InvoiceSubscribesPaymentAuthorized(Invoice invoice) {
        super(invoice.getAggregateId(),
                0,
                PaymentAuthorizedEvent.class.getSimpleName());
        
    }

    public InvoiceSubscribesPaymentAuthorized() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
