package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.Product;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.CategoryDeletedEvent;

public class ProductSubscribesCategoryDeleted extends EventSubscription {
    public ProductSubscribesCategoryDeleted(Product product) {
        super(product.getAggregateId(), 0, CategoryDeletedEvent.class.getSimpleName());
    }
}
