package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.Payment;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderCancelledEvent;

public class PaymentSubscribesOrderCancelled extends EventSubscription {
    

    public PaymentSubscribesOrderCancelled(Payment payment) {
        super(payment.getAggregateId(),
                0,
                OrderCancelledEvent.class.getSimpleName());
        
    }

    public PaymentSubscribesOrderCancelled() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
