package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class OrderDeletedEvent extends Event {

    public OrderDeletedEvent() {
        super();
    }

    public OrderDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}
