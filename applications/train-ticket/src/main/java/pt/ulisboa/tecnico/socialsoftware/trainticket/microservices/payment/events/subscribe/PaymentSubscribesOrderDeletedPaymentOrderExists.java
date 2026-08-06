package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.PaymentOrder;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.OrderDeletedEvent;


public class PaymentSubscribesOrderDeletedPaymentOrderExists extends EventSubscription {
    public PaymentSubscribesOrderDeletedPaymentOrderExists(PaymentOrder order) {
        super(order.getOrderAggregateId(),
                order.getOrderVersion(),
                OrderDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
