package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.Booking;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.UserDeletedEvent;

public class BookingSubscribesUserDeleted extends EventSubscription {
    public BookingSubscribesUserDeleted(Booking booking) {
        super(booking.getAggregateId(), 0, UserDeletedEvent.class.getSimpleName());
    }
}
