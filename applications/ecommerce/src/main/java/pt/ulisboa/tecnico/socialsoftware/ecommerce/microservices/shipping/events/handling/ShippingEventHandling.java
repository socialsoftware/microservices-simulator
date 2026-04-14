package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.eventProcessing.ShippingEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.ShippingRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.events.handling.handlers.PaymentAuthorizedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.PaymentAuthorizedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.events.handling.handlers.OrderCancelledEventHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderCancelledEvent;

@Component
public class ShippingEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private ShippingEventProcessing shippingEventProcessing;
    @Autowired
    private ShippingRepository shippingRepository;

    @Scheduled(fixedDelay = 1000)
    public void handlePaymentAuthorizedEventEvents() {
        eventApplicationService.handleSubscribedEvent(PaymentAuthorizedEvent.class,
                new PaymentAuthorizedEventHandler(shippingRepository, shippingEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleOrderCancelledEventEvents() {
        eventApplicationService.handleSubscribedEvent(OrderCancelledEvent.class,
                new OrderCancelledEventHandler(shippingRepository, shippingEventProcessing));
    }

}