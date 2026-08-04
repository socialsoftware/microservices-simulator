package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.eventProcessing.PriceConfigEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TrainDeletedEvent;

public class TrainDeletedEventHandler extends PriceConfigEventHandler {
    public TrainDeletedEventHandler(PriceConfigRepository priceconfigRepository, PriceConfigEventProcessing priceconfigEventProcessing) {
        super(priceconfigRepository, priceconfigEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.priceconfigEventProcessing.processTrainDeletedEvent(subscriberAggregateId, (TrainDeletedEvent) event);
    }
}
