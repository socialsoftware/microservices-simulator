package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.Payment;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderPlacedEvent;

public class PaymentSubscribesOrderPlaced extends EventSubscription {
    

    public PaymentSubscribesOrderPlaced(Payment payment) {
        super(payment.getAggregateId(),
                0,
                OrderPlacedEvent.class.getSimpleName());
        
    }

    public PaymentSubscribesOrderPlaced() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
