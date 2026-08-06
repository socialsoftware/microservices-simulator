package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfig;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TrainDeletedEvent;

public class PriceConfigSubscribesTrainDeleted extends EventSubscription {
    public PriceConfigSubscribesTrainDeleted(PriceConfig priceconfig) {
        super(priceconfig.getAggregateId(), 0L, TrainDeletedEvent.class.getSimpleName());
    }
}
