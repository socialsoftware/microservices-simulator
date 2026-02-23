package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.OrderUser;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.events.publish.UserUpdatedEvent;

public class OrderSubscribesUserUpdated extends EventSubscription {
    

    public OrderSubscribesUserUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
