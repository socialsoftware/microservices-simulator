package pt.ulisboa.tecnico.socialsoftware.showcase.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class BookingDeletedEvent extends Event {

    public BookingDeletedEvent() {
        super();
    }

    public BookingDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}