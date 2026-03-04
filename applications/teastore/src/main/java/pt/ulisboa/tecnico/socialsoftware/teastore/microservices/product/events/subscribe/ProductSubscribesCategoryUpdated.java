package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.ProductCategory;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.CategoryUpdatedEvent;

public class ProductSubscribesCategoryUpdated extends EventSubscription {
    

    public ProductSubscribesCategoryUpdated(ProductCategory productCategory) {
        super(productCategory.getCategoryAggregateId(),
                productCategory.getCategoryVersion(),
                CategoryUpdatedEvent.class.getSimpleName());
        
    }

    public ProductSubscribesCategoryUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
