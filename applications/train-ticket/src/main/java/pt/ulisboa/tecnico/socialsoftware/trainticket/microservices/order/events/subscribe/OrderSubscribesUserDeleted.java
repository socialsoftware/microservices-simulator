package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.UserDeletedEvent;

public class OrderSubscribesUserDeleted extends EventSubscription {
    public OrderSubscribesUserDeleted(Order order) {
        super(order.getAggregateId(), 0, UserDeletedEvent.class.getSimpleName());
    }
}
