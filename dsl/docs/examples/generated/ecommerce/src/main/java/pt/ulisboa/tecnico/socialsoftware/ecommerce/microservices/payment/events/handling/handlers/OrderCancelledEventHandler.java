package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.eventProcessing.PaymentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.PaymentRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderCancelledEvent;

public class OrderCancelledEventHandler extends PaymentEventHandler {
    public OrderCancelledEventHandler(PaymentRepository paymentRepository, PaymentEventProcessing paymentEventProcessing) {
        super(paymentRepository, paymentEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.paymentEventProcessing.processOrderCancelledEvent(subscriberAggregateId, (OrderCancelledEvent) event);
    }
}
