package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.Payment;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderDeletedEvent;

public class PaymentSubscribesOrderDeleted extends EventSubscription {
    public PaymentSubscribesOrderDeleted(Payment payment) {
        super(payment.getAggregateId(), 0, OrderDeletedEvent.class.getSimpleName());
    }
}
