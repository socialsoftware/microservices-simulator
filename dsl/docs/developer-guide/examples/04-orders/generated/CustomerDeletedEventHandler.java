package com.generated.abstractions.microservices.order.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import com.generated.abstractions.coordination.eventProcessing.OrderEventProcessing;
import com.generated.abstractions.microservices.order.aggregate.OrderRepository;
import com.generated.abstractions.microservices.customer.events.publish.CustomerDeletedEvent;

public class CustomerDeletedEventHandler extends OrderEventHandler {
    public CustomerDeletedEventHandler(OrderRepository orderRepository, OrderEventProcessing orderEventProcessing) {
        super(orderRepository, orderEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.orderEventProcessing.processCustomerDeletedEvent(subscriberAggregateId, (CustomerDeletedEvent) event);
    }
}
