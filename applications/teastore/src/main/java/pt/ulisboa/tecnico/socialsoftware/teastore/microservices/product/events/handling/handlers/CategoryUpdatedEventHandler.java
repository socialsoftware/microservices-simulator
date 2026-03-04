package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.coordination.eventProcessing.ProductEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.ProductRepository;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.CategoryUpdatedEvent;

public class CategoryUpdatedEventHandler extends ProductEventHandler {
    public CategoryUpdatedEventHandler(ProductRepository productRepository, ProductEventProcessing productEventProcessing) {
        super(productRepository, productEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.productEventProcessing.processCategoryUpdatedEvent(subscriberAggregateId, (CategoryUpdatedEvent) event);
    }
}
