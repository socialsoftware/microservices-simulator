package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.eventProcessing.OrderEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TripDeletedEvent;

public class TripDeletedEventHandler extends OrderEventHandler {
    public TripDeletedEventHandler(OrderRepository orderRepository, OrderEventProcessing orderEventProcessing) {
        super(orderRepository, orderEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.orderEventProcessing.processTripDeletedEvent(subscriberAggregateId, (TripDeletedEvent) event);
    }
}
