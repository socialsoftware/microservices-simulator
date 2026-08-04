package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class RouteUpdatedEvent extends Event {

    public RouteUpdatedEvent() {
        super();
    }

    public RouteUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}
