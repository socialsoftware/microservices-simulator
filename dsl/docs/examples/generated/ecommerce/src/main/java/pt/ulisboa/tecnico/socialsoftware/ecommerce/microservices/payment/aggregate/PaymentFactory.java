package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.PaymentDto;

public interface PaymentFactory {
    Payment createPayment(Integer aggregateId, PaymentDto paymentDto);
    Payment createPaymentFromExisting(Payment existingPayment);
    PaymentDto createPaymentDto(Payment payment);
}
