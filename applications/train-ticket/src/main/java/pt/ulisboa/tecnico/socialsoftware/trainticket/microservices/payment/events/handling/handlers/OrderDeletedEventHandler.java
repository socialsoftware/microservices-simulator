package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.PaymentRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.coordination.eventProcessing.PaymentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.OrderDeletedEvent;

public class OrderDeletedEventHandler extends PaymentEventHandler {
    public OrderDeletedEventHandler(PaymentRepository paymentRepository, PaymentEventProcessing paymentEventProcessing) {
        super(paymentRepository, paymentEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.paymentEventProcessing.processOrderDeletedEvent(subscriberAggregateId, (OrderDeletedEvent) event);
    }
}
