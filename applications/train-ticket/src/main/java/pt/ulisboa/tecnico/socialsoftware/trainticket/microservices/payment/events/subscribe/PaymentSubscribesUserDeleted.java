package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.Payment;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.UserDeletedEvent;

public class PaymentSubscribesUserDeleted extends EventSubscription {
    public PaymentSubscribesUserDeleted(Payment payment) {
        super(payment.getAggregateId(), 0, UserDeletedEvent.class.getSimpleName());
    }
}
