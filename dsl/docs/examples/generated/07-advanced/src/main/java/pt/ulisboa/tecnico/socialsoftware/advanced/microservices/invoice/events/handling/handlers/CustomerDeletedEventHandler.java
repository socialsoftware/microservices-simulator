package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.InvoiceRepository;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.coordination.eventProcessing.InvoiceEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.CustomerDeletedEvent;

public class CustomerDeletedEventHandler extends InvoiceEventHandler {
    public CustomerDeletedEventHandler(InvoiceRepository invoiceRepository, InvoiceEventProcessing invoiceEventProcessing) {
        super(invoiceRepository, invoiceEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.invoiceEventProcessing.processCustomerDeletedEvent(subscriberAggregateId, (CustomerDeletedEvent) event);
    }
}
