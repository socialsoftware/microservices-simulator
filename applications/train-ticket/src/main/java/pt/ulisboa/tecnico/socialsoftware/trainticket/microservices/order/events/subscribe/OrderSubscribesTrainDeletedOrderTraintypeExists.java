package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderTrain;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TrainDeletedEvent;


public class OrderSubscribesTrainDeletedOrderTraintypeExists extends EventSubscription {
    public OrderSubscribesTrainDeletedOrderTraintypeExists(OrderTrain train) {
        super(train.getTrainAggregateId(),
                train.getTrainVersion(),
                TrainDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
