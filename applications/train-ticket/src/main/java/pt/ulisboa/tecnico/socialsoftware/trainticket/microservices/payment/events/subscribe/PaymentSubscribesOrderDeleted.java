package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.Payment;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.OrderDeletedEvent;

public class PaymentSubscribesOrderDeleted extends EventSubscription {
    public PaymentSubscribesOrderDeleted(Payment payment) {
        super(payment.getAggregateId(), 0, OrderDeletedEvent.class.getSimpleName());
    }
}
