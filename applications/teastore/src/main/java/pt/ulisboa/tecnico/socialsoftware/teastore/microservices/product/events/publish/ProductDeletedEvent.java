package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ProductDeletedEvent extends Event {

    public ProductDeletedEvent() {
    }

    public ProductDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

}