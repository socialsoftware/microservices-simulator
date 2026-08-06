package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderTrip;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TripDeletedEvent;


public class OrderSubscribesTripDeletedOrderTripExists extends EventSubscription {
    public OrderSubscribesTripDeletedOrderTripExists(OrderTrip trip) {
        super(trip.getTripAggregateId(),
                trip.getTripVersion(),
                TripDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
