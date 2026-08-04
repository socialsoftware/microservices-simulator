package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.StationDeletedEvent;

public class OrderSubscribesStationDeleted extends EventSubscription {
    public OrderSubscribesStationDeleted(Order order) {
        super(order.getAggregateId(), 0, StationDeletedEvent.class.getSimpleName());
    }
}
