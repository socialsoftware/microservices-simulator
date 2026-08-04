package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigRoute;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.RouteDeletedEvent;


public class PriceConfigSubscribesRouteDeletedPriceConfigRouteExists extends EventSubscription {
    public PriceConfigSubscribesRouteDeletedPriceConfigRouteExists(PriceConfigRoute route) {
        super(route.getRouteAggregateId(),
                route.getRouteVersion(),
                RouteDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
