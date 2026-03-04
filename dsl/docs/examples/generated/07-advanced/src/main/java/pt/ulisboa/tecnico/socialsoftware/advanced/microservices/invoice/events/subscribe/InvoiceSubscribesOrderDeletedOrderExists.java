package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.InvoiceOrder;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.OrderDeletedEvent;


public class InvoiceSubscribesOrderDeletedOrderExists extends EventSubscription {
    public InvoiceSubscribesOrderDeletedOrderExists(InvoiceOrder order) {
        super(order.getOrderAggregateId(),
                order.getOrderVersion(),
                OrderDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
