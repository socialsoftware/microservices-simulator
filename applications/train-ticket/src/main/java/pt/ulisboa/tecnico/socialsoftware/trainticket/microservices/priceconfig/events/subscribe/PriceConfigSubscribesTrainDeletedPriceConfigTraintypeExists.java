package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigTrain;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TrainDeletedEvent;


public class PriceConfigSubscribesTrainDeletedPriceConfigTraintypeExists extends EventSubscription {
    public PriceConfigSubscribesTrainDeletedPriceConfigTraintypeExists(PriceConfigTrain trainType) {
        super(trainType.getTrainAggregateId(),
                trainType.getTrainVersion(),
                TrainDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
