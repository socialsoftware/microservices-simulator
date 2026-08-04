package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.PaymentRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.coordination.eventProcessing.PaymentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.UserDeletedEvent;

public class UserDeletedEventHandler extends PaymentEventHandler {
    public UserDeletedEventHandler(PaymentRepository paymentRepository, PaymentEventProcessing paymentEventProcessing) {
        super(paymentRepository, paymentEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.paymentEventProcessing.processUserDeletedEvent(subscriberAggregateId, (UserDeletedEvent) event);
    }
}
