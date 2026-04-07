package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.Payment;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.PaymentDto;

@Entity
public class SagaPayment extends Payment implements SagaAggregate {
    private SagaState sagaState;

    public SagaPayment() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaPayment(SagaPayment other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaPayment(Integer aggregateId, PaymentDto paymentDto) {
        super(aggregateId, paymentDto);
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