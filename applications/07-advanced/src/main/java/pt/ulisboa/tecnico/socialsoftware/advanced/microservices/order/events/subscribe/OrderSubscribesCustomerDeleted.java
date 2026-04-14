package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.CustomerDeletedEvent;

public class OrderSubscribesCustomerDeleted extends EventSubscription {
    public OrderSubscribesCustomerDeleted(Order order) {
        super(order.getAggregateId(), 0, CustomerDeletedEvent.class.getSimpleName());
    }
}
