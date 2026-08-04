package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfig;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.RouteDeletedEvent;

public class PriceConfigSubscribesRouteDeleted extends EventSubscription {
    public PriceConfigSubscribesRouteDeleted(PriceConfig priceconfig) {
        super(priceconfig.getAggregateId(), 0, RouteDeletedEvent.class.getSimpleName());
    }
}
