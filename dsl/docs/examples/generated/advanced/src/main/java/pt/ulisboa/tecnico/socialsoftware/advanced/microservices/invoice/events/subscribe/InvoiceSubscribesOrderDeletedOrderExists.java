package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.InvoiceOrder;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish.OrderDeletedEvent;


public class InvoiceSubscribesOrderDeletedOrderExists extends EventSubscription {
    public InvoiceSubscribesOrderDeletedOrderExists(InvoiceOrder order) {
        super(order.getOrderAggregateId(),
                order.getOrderVersion(),
                OrderDeletedEvent.class);
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
