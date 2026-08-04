package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class TripDeletedEvent extends Event {

    public TripDeletedEvent() {
        super();
    }

    public TripDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}
