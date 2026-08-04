package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PaymentDto;

public interface PaymentFactory {
    Payment createPayment(Integer aggregateId, PaymentDto paymentDto);
    Payment createPaymentFromExisting(Payment existingPayment);
    PaymentDto createPaymentDto(Payment payment);
}
