package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class PriceConfigDeletedEvent extends Event {

    public PriceConfigDeletedEvent() {
        super();
    }

    public PriceConfigDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}
