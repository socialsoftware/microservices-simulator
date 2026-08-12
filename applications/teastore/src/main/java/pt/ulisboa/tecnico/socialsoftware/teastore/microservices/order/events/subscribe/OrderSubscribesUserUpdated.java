package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.OrderUser;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.UserUpdatedEvent;

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
