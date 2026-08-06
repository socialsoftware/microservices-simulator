package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TripDeletedEvent;

public class OrderSubscribesTripDeleted extends EventSubscription {
    public OrderSubscribesTripDeleted(Order order) {
        super(order.getAggregateId(), 0L, TripDeletedEvent.class.getSimpleName());
    }
}
