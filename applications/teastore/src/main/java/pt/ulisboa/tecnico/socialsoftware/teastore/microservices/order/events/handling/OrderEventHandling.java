package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.eventProcessing.OrderEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.OrderRepository;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.events.handling.handlers.UserUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.events.publish.UserUpdatedEvent;

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