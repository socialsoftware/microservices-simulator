package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.coordination.eventProcessing.InvoiceEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.Invoice;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.InvoiceRepository;

public abstract class InvoiceEventHandler extends EventHandler {
    private InvoiceRepository invoiceRepository;
    protected InvoiceEventProcessing invoiceEventProcessing;

    public InvoiceEventHandler(InvoiceRepository invoiceRepository, InvoiceEventProcessing invoiceEventProcessing) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceEventProcessing = invoiceEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return invoiceRepository.findAll().stream().map(Invoice::getAggregateId).collect(Collectors.toSet());
    }

}
