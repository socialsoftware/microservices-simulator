package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.Invoice;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.InvoiceDto;

@Entity
public class SagaInvoice extends Invoice implements SagaAggregate {
    private SagaState sagaState;

    public SagaInvoice() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaInvoice(SagaInvoice other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaInvoice(Integer aggregateId, InvoiceDto invoiceDto) {
        super(aggregateId, invoiceDto);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = state;
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }
}