package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.OrderUser;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserUpdatedEvent;

public class OrderSubscribesUserUpdated extends EventSubscription {
    

    public OrderSubscribesUserUpdated(OrderUser orderUser) {
        super(orderUser.getUserAggregateId(),
                orderUser.getUserVersion(),
                UserUpdatedEvent.class.getSimpleName());
        
    }

    public OrderSubscribesUserUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
