package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.eventProcessing.PriceConfigEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.RouteDeletedEvent;

public class RouteDeletedEventHandler extends PriceConfigEventHandler {
    public RouteDeletedEventHandler(PriceConfigRepository priceconfigRepository, PriceConfigEventProcessing priceconfigEventProcessing) {
        super(priceconfigRepository, priceconfigEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.priceconfigEventProcessing.processRouteDeletedEvent(subscriberAggregateId, (RouteDeletedEvent) event);
    }
}
