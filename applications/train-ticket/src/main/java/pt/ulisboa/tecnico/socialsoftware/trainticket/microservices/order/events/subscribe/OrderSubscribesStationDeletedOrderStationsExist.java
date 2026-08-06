package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderFromStation;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.StationDeletedEvent;


public class OrderSubscribesStationDeletedOrderStationsExist extends EventSubscription {
    public OrderSubscribesStationDeletedOrderStationsExist(OrderFromStation fromStation) {
        super(fromStation.getStationAggregateId(),
                fromStation.getStationVersion(),
                StationDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
