package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.Invoice;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.Invoice;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.sagas.aggregates.SagaInvoice;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaInvoiceDto extends InvoiceDto {
private SagaState sagaState;

public SagaInvoiceDto(Invoice invoice) {
super((Invoice) invoice);
this.sagaState = ((SagaInvoice)invoice).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}