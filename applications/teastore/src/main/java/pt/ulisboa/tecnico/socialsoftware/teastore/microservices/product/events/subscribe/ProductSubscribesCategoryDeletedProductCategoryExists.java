package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.ProductCategory;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.CategoryDeletedEvent;


public class ProductSubscribesCategoryDeletedProductCategoryExists extends EventSubscription {
    public ProductSubscribesCategoryDeletedProductCategoryExists(ProductCategory productCategory) {
        super(productCategory.getCategoryAggregateId(),
                productCategory.getCategoryVersion(),
                CategoryDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
