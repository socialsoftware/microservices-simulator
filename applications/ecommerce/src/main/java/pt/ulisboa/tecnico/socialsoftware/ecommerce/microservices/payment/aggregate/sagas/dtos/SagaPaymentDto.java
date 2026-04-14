package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.Payment;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.Payment;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.PaymentDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.sagas.SagaPayment;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaPaymentDto extends PaymentDto {
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