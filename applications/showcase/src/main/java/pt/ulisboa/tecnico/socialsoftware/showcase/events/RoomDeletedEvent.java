package pt.ulisboa.tecnico.socialsoftware.showcase.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class RoomDeletedEvent extends Event {

    public RoomDeletedEvent() {
        super();
    }

    public RoomDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}