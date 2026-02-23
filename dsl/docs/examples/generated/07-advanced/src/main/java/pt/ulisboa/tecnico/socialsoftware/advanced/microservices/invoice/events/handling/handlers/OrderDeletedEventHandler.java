package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.InvoiceRepository;
import pt.ulisboa.tecnico.socialsoftware.advanced.coordination.eventProcessing.InvoiceEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish.OrderDeletedEvent;

public class OrderDeletedEventHandler extends InvoiceEventHandler {
    public OrderDeletedEventHandler(InvoiceRepository invoiceRepository, InvoiceEventProcessing invoiceEventProcessing) {
        super(invoiceRepository, invoiceEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.invoiceEventProcessing.processOrderDeletedEvent(subscriberAggregateId, (OrderDeletedEvent) event);
    }
}
