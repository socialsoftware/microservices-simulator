package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.sagas.dtos;

import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.Payment;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.Payment;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PaymentDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.sagas.SagaPayment;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public class SagaPaymentDto extends PaymentDto {
@Convert(converter = SagaStateConverter.class)
private SagaState sagaState;

public SagaPaymentDto(Payment payment) {
super((Payment) payment);
this.sagaState = ((SagaPayment)payment).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}