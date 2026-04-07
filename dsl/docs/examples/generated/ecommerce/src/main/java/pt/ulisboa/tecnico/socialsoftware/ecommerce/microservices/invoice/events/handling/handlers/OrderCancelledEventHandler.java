package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.coordination.eventProcessing.InvoiceEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.InvoiceRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderCancelledEvent;

public class OrderCancelledEventHandler extends InvoiceEventHandler {
    public OrderCancelledEventHandler(InvoiceRepository invoiceRepository, InvoiceEventProcessing invoiceEventProcessing) {
        super(invoiceRepository, invoiceEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.invoiceEventProcessing.processOrderCancelledEvent(subscriberAggregateId, (OrderCancelledEvent) event);
    }
}
