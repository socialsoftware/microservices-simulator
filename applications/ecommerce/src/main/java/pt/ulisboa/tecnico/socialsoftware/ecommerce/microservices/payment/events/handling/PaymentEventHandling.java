package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.eventProcessing.PaymentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.PaymentRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.events.handling.handlers.OrderPlacedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderPlacedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.events.handling.handlers.OrderCancelledEventHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderCancelledEvent;

@Component
public class PaymentEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private PaymentEventProcessing paymentEventProcessing;
    @Autowired
    private PaymentRepository paymentRepository;

    @Scheduled(fixedDelay = 1000)
    public void handleOrderPlacedEventEvents() {
        eventApplicationService.handleSubscribedEvent(OrderPlacedEvent.class,
                new OrderPlacedEventHandler(paymentRepository, paymentEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleOrderCancelledEventEvents() {
        eventApplicationService.handleSubscribedEvent(OrderCancelledEvent.class,
                new OrderCancelledEventHandler(paymentRepository, paymentEventProcessing));
    }

}