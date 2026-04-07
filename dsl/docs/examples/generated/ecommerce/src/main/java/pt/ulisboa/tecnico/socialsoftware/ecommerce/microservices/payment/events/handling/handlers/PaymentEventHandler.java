package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.eventProcessing.PaymentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.Payment;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.PaymentRepository;

public abstract class PaymentEventHandler extends EventHandler {
    private PaymentRepository paymentRepository;
    protected PaymentEventProcessing paymentEventProcessing;

    public PaymentEventHandler(PaymentRepository paymentRepository, PaymentEventProcessing paymentEventProcessing) {
        this.paymentRepository = paymentRepository;
        this.paymentEventProcessing = paymentEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return paymentRepository.findAll().stream().map(Payment::getAggregateId).collect(Collectors.toSet());
    }

}
