package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TrainDeletedEvent;

public class OrderSubscribesTrainDeleted extends EventSubscription {
    public OrderSubscribesTrainDeleted(Order order) {
        super(order.getAggregateId(), 0L, TrainDeletedEvent.class.getSimpleName());
    }
}
