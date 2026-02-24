package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;

public class ProductSubscribesCategoryUpdated extends EventSubscription {
    

    public ProductSubscribesCategoryUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
