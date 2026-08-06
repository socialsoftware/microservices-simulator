package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripTrain;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TrainDeletedEvent;


public class TripSubscribesTrainDeletedTripTraintypeExists extends EventSubscription {
    public TripSubscribesTrainDeletedTripTraintypeExists(TripTrain trainType) {
        super(trainType.getTrainAggregateId(),
                trainType.getTrainVersion(),
                TrainDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
