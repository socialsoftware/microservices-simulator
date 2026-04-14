package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.ShippingRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.eventProcessing.ShippingEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderDeletedEvent;

public class OrderDeletedEventHandler extends ShippingEventHandler {
    public OrderDeletedEventHandler(ShippingRepository shippingRepository, ShippingEventProcessing shippingEventProcessing) {
        super(shippingRepository, shippingEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.shippingEventProcessing.processOrderDeletedEvent(subscriberAggregateId, (OrderDeletedEvent) event);
    }
}
