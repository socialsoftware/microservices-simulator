package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.InvoiceDto;

public interface InvoiceFactory {
    Invoice createInvoice(Integer aggregateId, InvoiceDto invoiceDto);
    Invoice createInvoiceFromExisting(Invoice existingInvoice);
    InvoiceDto createInvoiceDto(Invoice invoice);
}
