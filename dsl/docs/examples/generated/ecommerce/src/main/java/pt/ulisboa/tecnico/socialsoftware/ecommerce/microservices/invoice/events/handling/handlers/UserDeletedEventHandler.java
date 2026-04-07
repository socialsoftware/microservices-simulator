package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.InvoiceRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.coordination.eventProcessing.InvoiceEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserDeletedEvent;

public class UserDeletedEventHandler extends InvoiceEventHandler {
    public UserDeletedEventHandler(InvoiceRepository invoiceRepository, InvoiceEventProcessing invoiceEventProcessing) {
        super(invoiceRepository, invoiceEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.invoiceEventProcessing.processUserDeletedEvent(subscriberAggregateId, (UserDeletedEvent) event);
    }
}
