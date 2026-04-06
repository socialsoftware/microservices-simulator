package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.coordination.functionalities.InvoiceFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.coordination.webapi.requestDtos.CreateInvoiceRequestDto;

@RestController
public class InvoiceController {
    @Autowired
    private InvoiceFunctionalities invoiceFunctionalities;

    @PostMapping("/invoices/create")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceDto createInvoice(@RequestBody CreateInvoiceRequestDto createRequest) {
        return invoiceFunctionalities.createInvoice(createRequest);
    }

    @GetMapping("/invoices/{invoiceAggregateId}")
    public InvoiceDto getInvoiceById(@PathVariable Integer invoiceAggregateId) {
        return invoiceFunctionalities.getInvoiceById(invoiceAggregateId);
    }

    @PutMapping("/invoices")
    public InvoiceDto updateInvoice(@RequestBody InvoiceDto invoiceDto) {
        return invoiceFunctionalities.updateInvoice(invoiceDto);
    }

    @DeleteMapping("/invoices/{invoiceAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvoice(@PathVariable Integer invoiceAggregateId) {
        invoiceFunctionalities.deleteInvoice(invoiceAggregateId);
    }

    @GetMapping("/invoices")
    public List<InvoiceDto> getAllInvoices() {
        return invoiceFunctionalities.getAllInvoices();
    }
}
