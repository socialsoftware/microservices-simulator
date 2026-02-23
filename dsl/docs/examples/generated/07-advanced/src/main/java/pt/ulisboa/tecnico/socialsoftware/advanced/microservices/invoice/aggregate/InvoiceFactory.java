package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate;

import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceDto;

public interface InvoiceFactory {
    Invoice createInvoice(Integer aggregateId, InvoiceDto invoiceDto);
    Invoice createInvoiceFromExisting(Invoice existingInvoice);
    InvoiceDto createInvoiceDto(Invoice invoice);
}
