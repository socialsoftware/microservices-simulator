package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.UserDeletedEvent;

public class OrderSubscribesUserDeleted extends EventSubscription {
    public OrderSubscribesUserDeleted(Order order) {
        super(order.getAggregateId(), 0L, UserDeletedEvent.class.getSimpleName());
    }
}
