package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.eventProcessing.OrderEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TrainDeletedEvent;

public class TrainDeletedEventHandler extends OrderEventHandler {
    public TrainDeletedEventHandler(OrderRepository orderRepository, OrderEventProcessing orderEventProcessing) {
        super(orderRepository, orderEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.orderEventProcessing.processTrainDeletedEvent(subscriberAggregateId, (TrainDeletedEvent) event);
    }
}
