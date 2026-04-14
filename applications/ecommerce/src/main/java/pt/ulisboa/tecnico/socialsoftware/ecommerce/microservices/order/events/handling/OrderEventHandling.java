package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.coordination.eventProcessing.OrderEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.OrderRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.events.handling.handlers.UserUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserUpdatedEvent;

@Component
public class OrderEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private OrderEventProcessing orderEventProcessing;
    @Autowired
    private OrderRepository orderRepository;

    @Scheduled(fixedDelay = 1000)
    public void handleUserUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(UserUpdatedEvent.class,
                new UserUpdatedEventHandler(orderRepository, orderEventProcessing));
    }

}