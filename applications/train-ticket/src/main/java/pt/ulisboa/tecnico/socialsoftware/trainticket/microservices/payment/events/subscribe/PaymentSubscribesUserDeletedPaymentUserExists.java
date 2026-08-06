package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.PaymentUser;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.UserDeletedEvent;


public class PaymentSubscribesUserDeletedPaymentUserExists extends EventSubscription {
    public PaymentSubscribesUserDeletedPaymentUserExists(PaymentUser user) {
        super(user.getUserAggregateId(),
                user.getUserVersion(),
                UserDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
