package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ProductDeletedEvent extends Event {

    public ProductDeletedEvent() {
        super();
    }

    public ProductDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}