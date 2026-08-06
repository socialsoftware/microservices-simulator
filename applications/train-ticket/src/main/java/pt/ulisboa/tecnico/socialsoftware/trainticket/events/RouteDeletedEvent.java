package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class RouteDeletedEvent extends Event {

    public RouteDeletedEvent() {
        super();
    }

    public RouteDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}
