package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class StationDeletedEvent extends Event {

    public StationDeletedEvent() {
        super();
    }

    public StationDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}
