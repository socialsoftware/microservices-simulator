package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.coordination.eventProcessing.ProductEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.ProductRepository;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.handling.handlers.CategoryUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.CategoryUpdatedEvent;

@Component
public class ProductEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private ProductEventProcessing productEventProcessing;
    @Autowired
    private ProductRepository productRepository;

    @Scheduled(fixedDelay = 1000)
    public void handleCategoryUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(CategoryUpdatedEvent.class,
                new CategoryUpdatedEventHandler(productRepository, productEventProcessing));
    }

}