package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.Invoice;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.InvoiceFactory;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.sagas.SagaInvoice;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.sagas.dtos.SagaInvoiceDto;

@Service
@Profile("sagas")
public class SagasInvoiceFactory implements InvoiceFactory {
    @Override
    public Invoice createInvoice(Integer aggregateId, InvoiceDto invoiceDto) {
        return new SagaInvoice(aggregateId, invoiceDto);
    }

    @Override
    public Invoice createInvoiceFromExisting(Invoice existingInvoice) {
        return new SagaInvoice((SagaInvoice) existingInvoice);
    }

    @Override
    public InvoiceDto createInvoiceDto(Invoice invoice) {
        return new SagaInvoiceDto(invoice);
    }
}