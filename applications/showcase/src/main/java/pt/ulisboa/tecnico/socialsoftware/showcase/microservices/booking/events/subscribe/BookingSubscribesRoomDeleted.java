package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.Booking;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.RoomDeletedEvent;

public class BookingSubscribesRoomDeleted extends EventSubscription {
    public BookingSubscribesRoomDeleted(Booking booking) {
        super(booking.getAggregateId(), 0, RoomDeletedEvent.class.getSimpleName());
    }
}
