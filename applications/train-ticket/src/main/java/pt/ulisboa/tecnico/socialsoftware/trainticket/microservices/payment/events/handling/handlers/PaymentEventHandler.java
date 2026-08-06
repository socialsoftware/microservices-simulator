package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.coordination.eventProcessing.PaymentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.PaymentRepository;

public abstract class PaymentEventHandler extends EventHandler {
    protected PaymentEventProcessing paymentEventProcessing;

    public PaymentEventHandler(PaymentRepository paymentRepository, PaymentEventProcessing paymentEventProcessing) {
        super(paymentRepository);
        this.paymentEventProcessing = paymentEventProcessing;
    }

}
