package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.OrderUser;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserDeletedEvent;


public class OrderSubscribesUserDeletedOrderUserExists extends EventSubscription {
    public OrderSubscribesUserDeletedOrderUserExists(OrderUser user) {
        super(user.getUserAggregateId(),
                user.getUserVersion(),
                UserDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
