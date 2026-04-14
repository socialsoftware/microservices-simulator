package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserDeletedEvent;

public class OrderSubscribesUserDeleted extends EventSubscription {
    public OrderSubscribesUserDeleted(Order order) {
        super(order.getAggregateId(), 0, UserDeletedEvent.class.getSimpleName());
    }
}
