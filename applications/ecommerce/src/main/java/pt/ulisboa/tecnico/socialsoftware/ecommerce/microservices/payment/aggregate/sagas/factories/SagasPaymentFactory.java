package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.Payment;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.PaymentDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.PaymentFactory;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.sagas.SagaPayment;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.sagas.dtos.SagaPaymentDto;

@Service
@Profile("sagas")
public class SagasPaymentFactory implements PaymentFactory {
    @Override
    public Payment createPayment(Integer aggregateId, PaymentDto paymentDto) {
        return new SagaPayment(aggregateId, paymentDto);
    }

    @Override
    public Payment createPaymentFromExisting(Payment existingPayment) {
        return new SagaPayment((SagaPayment) existingPayment);
    }

    @Override
    public PaymentDto createPaymentDto(Payment payment) {
        return new SagaPaymentDto(payment);
    }
}