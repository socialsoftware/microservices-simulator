package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.PaymentRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.eventProcessing.PaymentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderDeletedEvent;

public class OrderDeletedEventHandler extends PaymentEventHandler {
    public OrderDeletedEventHandler(PaymentRepository paymentRepository, PaymentEventProcessing paymentEventProcessing) {
        super(paymentRepository, paymentEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.paymentEventProcessing.processOrderDeletedEvent(subscriberAggregateId, (OrderDeletedEvent) event);
    }
}
